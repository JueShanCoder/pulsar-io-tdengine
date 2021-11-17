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
import org.apache.avro.generic.GenericData;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.schema.GenericRecord;
import org.apache.pulsar.io.core.PushSource;
import org.apache.pulsar.io.core.SourceContext;
import org.apache.pulsar.io.core.annotations.Connector;
import org.apache.pulsar.io.core.annotations.IOType;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import static org.apache.pulsar.io.tdengine.TDengineSourceConfig.driverName;

@Connector(
        name = "TDengineSource",
        type = IOType.SOURCE,
        help = "A TDengine source connector for Pulsar ",
        configClass = TDengineSourceConfig.class)
@Slf4j
public class TDengineSource extends PushSource<byte[]> {

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
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        log.info("columnlaber {} , resultset {}", metaData.getColumnLabel(i), resultSet.getString(i));
                        TDengineRecord tDengineRecord = new TDengineRecord(
                                Optional.ofNullable(metaData.getColumnLabel(i)), resultSet.getString(i).getBytes(StandardCharsets.UTF_8));
                        log.info("TDengineSource got message {}",new ObjectMapper().writeValueAsString(tDengineRecord));
//                        log.info("tDengineRecord properties {}", tDengineRecord.getProperties());
//                        tDengineRecord.getProperties().put(ACTION, INSERT);
                        consume(tDengineRecord);
                    }
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

    private String getTopicName(){
       return "TOPIC-" + snowflake.nextId();
    }
}