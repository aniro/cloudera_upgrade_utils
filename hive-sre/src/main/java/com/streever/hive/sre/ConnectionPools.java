package com.streever.hive.sre;

import com.streever.hive.config.Metastore;
import com.streever.hive.config.SreProcessesConfig;
import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionPools {
    private SreProcessesConfig config;

    private PoolingDataSource<PoolableConnection> metastoreDirectDataSource = null;
    private PoolingDataSource<PoolableConnection> hs2DataSource = null;

    public ConnectionPools(SreProcessesConfig config) {
        this.config = config;
    }

    public void init() {
        initMetastoreDataSource();
        initHs2DataSource();
    }

    public DataSource getMetastoreDirectDataSource() {
        if (metastoreDirectDataSource == null) {
            initMetastoreDataSource();
        }
        return metastoreDirectDataSource;
    }

    public DataSource getHs2DataSource() {
        if (hs2DataSource == null) {
            initHs2DataSource();
        }
        return hs2DataSource;
    }

    public Connection getMetastoreDirectConnection() throws SQLException {
        Connection conn = getMetastoreDirectDataSource().getConnection();
        if (this.config.getMetastoreDirect().getInitSql() != null) {
            conn.createStatement().execute(this.config.getMetastoreDirect().getInitSql());
        }
        return conn;
    }

    public Connection getHs2Connection() throws SQLException {
        Connection conn = getHs2DataSource().getConnection();
        return conn;
    }

    protected void initMetastoreDataSource() {
        // Metastore Direct
        if (config.getMetastoreDirect() == null) {
            throw new RuntimeException("Missing configuration to connect to Metastore RDBMS");
        }
        Metastore msdb = config.getMetastoreDirect();
        ConnectionFactory msconnectionFactory =
                new DriverManagerConnectionFactory(msdb.getUri(), msdb.getConnectionProperties());

        PoolableConnectionFactory mspoolableConnectionFactory =
                new PoolableConnectionFactory(msconnectionFactory, null);

        ObjectPool<PoolableConnection> msconnectionPool =
                new GenericObjectPool<>(mspoolableConnectionFactory);

        mspoolableConnectionFactory.setPool(msconnectionPool);

        this.metastoreDirectDataSource =
                new PoolingDataSource<>(msconnectionPool);

    }

    protected void initHs2DataSource() {
        // this is optional.
        if (config.getHs2() != null) {
            Metastore hs2db = config.getHs2();
            ConnectionFactory hs2connectionFactory =
                    new DriverManagerConnectionFactory(hs2db.getUri(), hs2db.getConnectionProperties());

            PoolableConnectionFactory hs2poolableConnectionFactory =
                    new PoolableConnectionFactory(hs2connectionFactory, null);

            ObjectPool<PoolableConnection> hs2connectionPool =
                    new GenericObjectPool<>(hs2poolableConnectionFactory);

            hs2poolableConnectionFactory.setPool(hs2connectionPool);

            this.hs2DataSource =
                    new PoolingDataSource<>(hs2connectionPool);
        }
    }

}
