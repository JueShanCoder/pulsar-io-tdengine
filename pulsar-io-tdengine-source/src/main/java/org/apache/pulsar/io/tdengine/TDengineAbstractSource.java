package org.apache.pulsar.io.tdengine;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.taosdata.jdbc.TSDBConnection;
import com.taosdata.jdbc.TSDBDriver;
import com.taosdata.jdbc.TSDBSubscribe;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.io.core.PushSource;
import org.apache.pulsar.io.core.SourceContext;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static org.apache.pulsar.io.tdengine.TDengineSourceConfig.driverName;

@Slf4j
public abstract class TDengineAbstractSource<V> extends PushSource<V> {

    private DruidDataSource dataSource = null;
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
        dataSource = new DruidDataSource();
        tDengineSourceConfig = TDengineSourceConfig.load(config);
        if (tDengineSourceConfig.getJdbcUrl() == null || tDengineSourceConfig.getUserName() == null ||
                tDengineSourceConfig.getPassword() == null || tDengineSourceConfig.getSql() == null )
            throw new IllegalArgumentException("Required params not set.");

        // jdbc properties
        dataSource.setDriverClassName(driverName);
        dataSource.setUrl(tDengineSourceConfig.getJdbcUrl());
        dataSource.setUsername(tDengineSourceConfig.getUserName());
        dataSource.setPassword(tDengineSourceConfig.getPassword());
        // properties
        Properties properties = new Properties();
        properties.setProperty(TSDBDriver.PROPERTY_KEY_CHARSET, tDengineSourceConfig.getCharset());
        properties.setProperty(TSDBDriver.PROPERTY_KEY_TIME_ZONE, tDengineSourceConfig.getTimezone());
        // pool configurations
        dataSource.setInitialSize(10);
        dataSource.setMinIdle(10);
        dataSource.setMaxActive(10);
        dataSource.setMaxWait(30000);
        dataSource.setValidationQuery("select server_status()");
        dataSource.setConnectProperties(properties);

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