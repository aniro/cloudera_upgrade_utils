package com.streever.hive.sre;

import com.streever.hive.reporting.Counter;
import com.streever.hive.reporting.ReportCounter;
import com.streever.sql.JDBCUtils;
import com.streever.sql.QueryDefinition;
import com.streever.sql.ResultArray;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.streever.hive.reporting.ReportCounter.*;

public abstract class MetastoreProcess extends SreProcessBase implements Counter, Runnable {

    private ReportCounter counter = new ReportCounter();

    @Override
    public void init(ProcessContainer parent, String outputDirectory) throws FileNotFoundException {
        setParent(parent);
        if (outputDirectory == null) {
            throw new RuntimeException("Config File and Output Directory must be set before init.");
        }
        setOutputDirectory(outputDirectory);
        setStatus(WAITING);
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        this.getCounter().setName(getUniqueName());
    }

    public ReportCounter getCounter() {
        return counter;
    }

    public void setCounter(ReportCounter counter) {
        this.counter = counter;
    }

    @Override
    public int getStatus() {
        return counter.getStatus();
    }

    @Override
    public String getStatusStr() {
        return counter.getStatusStr();
    }

    @Override
    public List<ReportCounter> getCounterChildren() {
        return counter.getChildren();
    }

    @Override
    public void setStatus(int status) {
        counter.setStatus(status);
    }

    @Override
    public void incProcessed(int increment) {
        counter.incProcessed(increment);
    }

    @Override
    public long getProcessed() {
        return counter.getProcessed();
    }

    @Override
    public void setProcessed(long processed) {
        counter.setProcessed(processed);
    }

    @Override
    public long getTotalCount() {
        return counter.getTotalCount();
    }

    @Override
    public void setTotalCount(long totalCount) {
        counter.setTotalCount(totalCount);
    }

    @Override
    public void incSuccess(int increment) {
        counter.incSuccess(increment);
    }

    @Override
    public long getSuccess() {
        return counter.getSuccess();
    }

    @Override
    public void setSuccess(long success) {
        counter.setSuccess(success);
    }

    @Override
    public void incError(int increment) {
        counter.incError(increment);
    }

    @Override
    public long getError() {
        return counter.getError();
    }

    @Override
    public void setError(long error) {
        counter.setError(error);
    }

    @Override
    public String toString() {
        return "MetastoreProcess{}";
    }
}
