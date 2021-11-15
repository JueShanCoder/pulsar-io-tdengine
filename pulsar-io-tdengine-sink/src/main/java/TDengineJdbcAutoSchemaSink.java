import org.apache.pulsar.io.core.annotations.Connector;
import org.apache.pulsar.io.core.annotations.IOType;
import org.apache.pulsar.io.jdbc.BaseJdbcAutoSchemaSink;
import org.apache.pulsar.io.jdbc.JdbcSinkConfig;

@Connector(
        name = "jdbc-tdengine",
        type = IOType.SINK,
        help = "A simple JDBC sink for TDengine that writes pulsar messages to a database table",
        configClass = JdbcSinkConfig.class
)
public class TDengineJdbcAutoSchemaSink extends BaseJdbcAutoSchemaSink {

}