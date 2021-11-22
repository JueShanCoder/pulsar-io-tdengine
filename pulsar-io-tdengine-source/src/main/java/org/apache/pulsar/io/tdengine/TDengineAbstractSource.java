package org.apache.pulsar.io.tdengine;


import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taosdata.jdbc.TSDBConnection;
import com.taosdata.jdbc.TSDBDriver;
import com.taosdata.jdbc.TSDBResultSet;
import com.taosdata.jdbc.TSDBSubscribe;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.io.core.PushSource;
import org.apache.pulsar.io.core.SourceContext;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

import static org.apache.pulsar.io.tdengine.TDengineSourceConfig.driverName;

@Slf4j
public abstract class TDengineAbstractSource<V> extends PushSource<V> {

    private DruidDataSource dataSource = null;
    private TDengineSourceConfig tDengineSourceConfig;
    private Thread thread;
    protected volatile boolean running = false;
    private final Thread.UncaughtExceptionHandler handler = (t, e) -> log.error("[{}] parse events has an error", t.getName(), e);
    private Connection connection;
    private TSDBSubscribe subscribe;
    private Snowflake snowflake;

    protected static final String ACTION = "ACTION";
    protected static final String INSERT = "INSERT";

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
        // TODO check default value
        properties.setProperty(TSDBDriver.PROPERTY_KEY_CHARSET, "UTF-8");
        properties.setProperty(TSDBDriver.PROPERTY_KEY_TIME_ZONE, "UTC-8");
//        properties.setProperty(TSDBDriver.PROPERTY_KEY_CHARSET, tDengineSourceConfig.getCharset());
//        properties.setProperty(TSDBDriver.PROPERTY_KEY_TIME_ZONE, tDengineSourceConfig.getTimezone());
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
            subscribe = unwrap.subscribe(getTopicName(), tDengineSourceConfig.getSql(), true);
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

    private void process(){
        try {
            while (running) {
                TSDBResultSet resultSet = subscribe.consume();
                if (resultSet == null) {
                    continue;
                }
                ResultSetMetaData metaData = resultSet.getMetaData();
                while (resultSet.next()) {
                    Map<String, Object> columnMap = new HashMap<>();
                    int columnCount = metaData.getColumnCount();

                    for (int i = 1; i <= columnCount; i++) {
                        columnMap.put(metaData.getColumnLabel(i),resultSet.getString(i));
                    }
                    log.info("TDengineAbstractSource got message {}",new ObjectMapper().writeValueAsString(columnMap));
                    TDengineRecord<V> tDengineRecord = new TDengineRecord<>();
                    tDengineRecord.setRecord(extractValue(columnMap));
                    consume(tDengineRecord);
                }
            }
        } catch (Exception e) {
            log.error("process error!", e);
        } finally {
            try {
                if (null != subscribe)
                    // close subscribe
                    subscribe.close(true);

                if (connection != null)
                    connection.close();

            } catch (SQLException e) {
                log.error("Failed to close connection or subscribe.");
            }
        }
    }

    public abstract V extractValue(Map<String, Object> columnMap);

    private String getTopicName(){
       return "TOPIC-" + snowflake.nextId();
    }

}