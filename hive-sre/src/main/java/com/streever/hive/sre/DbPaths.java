package com.streever.hive.sre;

import com.streever.hadoop.HadoopSession;
import com.streever.hadoop.shell.command.CommandReturn;
import com.streever.sql.JDBCUtils;
import com.streever.sql.QueryDefinition;
import com.streever.sql.ResultArray;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static com.streever.hive.reporting.ReportCounter.*;

public class DbPaths extends SRERunnable {

    private DbSetProcess parent;
    private HadoopSession cliSession;

    private List<CommandReturnCheck> checks = new ArrayList<CommandReturnCheck>();

    public DbSetProcess getParent() {
        return parent;
    }

    public void setParent(DbSetProcess parent) {
        this.parent = parent;
    }

    public HadoopSession getCliSession() {
        return cliSession;
    }

    public List<CommandReturnCheck> getChecks() {
        return checks;
    }

    public void setChecks(List<CommandReturnCheck> checks) {
        this.checks = checks;
    }

    public DbPaths(String name, DbSetProcess dbSet) {
        setName(name);
        setParent(dbSet);
    }

    @Override
    public Boolean init() {
        Boolean rtn = Boolean.FALSE;
        for (CommandReturnCheck check : parent.getChecks()) {
            try {
                CommandReturnCheck newCheck = (CommandReturnCheck) check.clone();
                checks.add(newCheck);
                // Connect CommandReturnCheck counter to this counter as a child.
                // TODO: Need to set Counters name from the 'check'
                getCounter().addChild(newCheck.getCounter());
                // Redirect Output.
                if (newCheck.getErrorFilename() == null) {
                    newCheck.setErrorStream(this.error);
                }
                if (newCheck.getSuccessFilename() == null) {
                    newCheck.setSuccessStream(this.success);
                }
                // TODO: Set success and error printstreams to output files.
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        this.cliSession = HadoopSession.get("DB Paths for: " + getName() + UUID.randomUUID());
        String[] api = {"-api"};
        try {
            // TODO: Need to promote failure when keytab doesn't exist.
//            System.out.println("Start CLI Session");
            rtn = this.cliSession.start(api);
//            System.out.println("CLI Session Started");
        } catch (Exception e) {
            System.err.println("Issue starting CLI Session:" + e.getMessage());
            e.printStackTrace();
        }
        return rtn;
    }

    @Override
    public void run() {
        this.setStatus(STARTED);
        QueryDefinition queryDefinition = null;
        try (Connection conn = getParent().getParent().getConnectionPools().
                getMetastoreDirectConnection()) {

            queryDefinition = getParent().getQueryDefinitions().
                    getQueryDefinition(getParent().getPathsListingQuery());
            PreparedStatement preparedStatement = JDBCUtils.getPreparedStatement(conn, queryDefinition);

            Properties overrides = new Properties();
            overrides.setProperty("dbs", getName());
            JDBCUtils.setPreparedStatementParameters(preparedStatement, queryDefinition, overrides);

            ResultSet epRs = preparedStatement.executeQuery();
            ResultArray rarray = new ResultArray(epRs);
            epRs.close();

            String[] columns = getParent().getListingColumns();

            String[][] columnsArray = rarray.getColumns(columns);
            this.setTotalCount(rarray.getCount() * this.getCounterChildren().size());
            // Loop through the paths
            if (columnsArray[0] != null && columnsArray[0].length > 0) {
                this.setStatus(PROCESSING);

                for (int i = 0; i < columnsArray[0].length; i++) { //String path : columnArray) {
                    String[] args = new String[columnsArray.length];
                    for (int a = 0; a < columnsArray.length; a++) {
                        if (columnsArray[a][i] != null)
                        args[a] = columnsArray[a][i];
                        else
                            args[a] = " "; // Prevent null in array.  Messes up String.format when array has nulls.
                    }
                    for (CommandReturnCheck lclCheck : getChecks()) {
                        try {
                            String rcmd = lclCheck.getFullCommand(args);
                            if (rcmd != null) {
                                CommandReturn cr = getCliSession().processInput(rcmd);
                                lclCheck.incProcessed(1);
                                if (!cr.isError() || (lclCheck.getInvertCheck() && cr.isError())) {
                                    lclCheck.onSuccess(cr);
                                    lclCheck.incSuccess(1);
                                    this.incSuccess(1);
                                } else {
                                    lclCheck.onError(cr);
                                    lclCheck.incError(1);
                                    this.incError(1);
                                }
                            }
                        } catch (RuntimeException t) {
                            // Malformed cli request.  Input is missing an element required to complete call.
                            // Unusual, but not an expection.
                        }
                    }
                    incProcessed(1);
                }
            }
        } catch (SQLException e) {
            System.err.println((queryDefinition != null)? queryDefinition.getStatement(): "Unknown");
            System.err.println("Failure in DbPaths");
            e.printStackTrace();
            setStatus(ERROR);
        } catch (Throwable t) {
            System.err.println("Failure in DbPaths");
            t.printStackTrace();
        }
        setStatus(COMPLETED);
    }

    @Override
    public String toString() {
        return "DbPathsProcess{}";
    }
}
