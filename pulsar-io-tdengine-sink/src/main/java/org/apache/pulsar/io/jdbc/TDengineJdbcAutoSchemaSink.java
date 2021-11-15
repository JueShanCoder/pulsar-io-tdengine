package org.apache.pulsar.io.jdbc;

import org.apache.pulsar.io.core.annotations.Connector;
import org.apache.pulsar.io.core.annotations.IOType;

@Connector(
        name = "jdbc-tdengine",
        type = IOType.SINK,
        help = "A simple JDBC sink for TDengine that writes pulsar messages to a database table",
        configClass = JdbcSinkConfig.class
)
public class TDengineJdbcAutoSchemaSink extends BaseJdbcAutoSchemaSink {

}