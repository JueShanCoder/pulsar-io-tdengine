package org.apache.pulsar.io.tdengine;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.taosdata.jdbc.TSDBConnection;
import com.taosdata.jdbc.TSDBSubscribe;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.io.core.PushSource;
import org.apache.pulsar.io.core.SourceContext;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

@Slf4j
public abstract class TDengineAbstractSource<V> extends PushSource<V> {

    private HikariDataSource dataSource = null;
    private Thread thread;
    private final Thread.UncaughtExceptionHandler handler = (t, e) -> log.error("[{}] parse events has an error", t.getName(), e);

    protected TDengineSourceConfig tDengineSourceConfig;
    protected volatile boolean running = false;
    protected Connection connection;
    protected TSDBSubscribe subscribe;
    protected Snowflake snowflake;
    protected String topicName;

    @Override
    public void open(Map<String, Object> config, SourceContext sourceContext) throws Exception {
        tDengineSourceConfig = TDengineSourceConfig.load(config);
        if (tDengineSourceConfig.getJdbcUrl() == null || tDengineSourceConfig.getUsername() == null ||
                tDengineSourceConfig.getPassword() == null || tDengineSourceConfig.getSql() == null )
            throw new IllegalArgumentException("Required params not set.");

        HikariConfig hikariConfig = new HikariConfig();
        // jdbc properties
        hikariConfig.setJdbcUrl(tDengineSourceConfig.getJdbcUrl());
        hikariConfig.setUsername(tDengineSourceConfig.getUsername());
        hikariConfig.setPassword(tDengineSourceConfig.getPassword());

        // by default
        // connection pool configurations
        hikariConfig.setMinimumIdle(10);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setMaxLifetime(0);
        hikariConfig.setIdleTimeout(0);
        hikariConfig.setConnectionTestQuery("select server_status()");
        dataSource = new HikariDataSource(hikariConfig);
        // snowflake
        snowflake = IdUtil.getSnowflake(1, 1);
        this.start();
    }

    @Override
    public void close() throws Exception {
        log.info("Close TDengine source");
        if (!running)
            return;
        running = false;
        if (thread != null) {
            thread.interrupt();
            thread.join();
        }
        if (null != subscribe) {
            subscribe.close(true);
        }
        if (connection != null){
            connection.close();
        }
    }

    protected void start() {
        try {
            connection = dataSource.getConnection();
            // create TDengine subscribe
            TSDBConnection unwrap = connection.unwrap(TSDBConnection.class);
            topicName = getTopicName();
            subscribe = unwrap.subscribe(topicName, tDengineSourceConfig.getSql(), tDengineSourceConfig.getRestart());
        } catch (SQLException e) {
            log.error("Failed to get a connection from the connection pool.",e);
            throw new IllegalArgumentException("Failed to get a connection from the connection pool.");
        }
        Objects.requireNonNull(dataSource, "connector is null");
        Objects.requireNonNull(subscribe, "subscribe is null");
        thread = new Thread(this::process);
        thread.setName("TDengine Source Thread");
        thread.setUncaughtExceptionHandler(handler);
        running = true;
        thread.start();
    }

    abstract void process();

    public abstract V extractValue(Map<String, Object> columnMap);

    private String getTopicName(){
       return "TOPIC-" + snowflake.nextId();
    }

}