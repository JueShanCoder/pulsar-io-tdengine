package org.apache.pulsar.io.tdengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import org.apache.pulsar.io.core.annotations.FieldDoc;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

@Data
public class TDengineSourceConfig implements Serializable {

    public static final String driverName = "com.taosdata.jdbc.TSDBDriver";

    @FieldDoc(
            required = false,
            defaultValue = "UTF-8",
            sensitive = true,
            help = "TSDB Driver properties key charset"
    )
    private String charset;

    @FieldDoc(
            required = false,
            defaultValue = "UTC-8",
            sensitive = true,
            help = "TSDB Driver properties key timezone"
    )
    private String timezone;

    @FieldDoc(
            required = true,
            defaultValue = "",
            sensitive = true,
            help = "The JDBC url of the database this connector connects to"
    )
    private String jdbcUrl;

    @FieldDoc(
            required = true,
            defaultValue = "",
            sensitive = true,
            help = "Username used to connect to the database specified by `jdbcUrl`"
    )
    private String userName;

    @FieldDoc(
            required = true,
            defaultValue = "",
            sensitive = true,
            help = "Password used to connect to the database specified by `jdbcUrl`"
    )
    private String password;

    @FieldDoc(
            required = false,
            defaultValue = "true",
            sensitive = true,
            help = "If restart is true, all data will be read, otherwise only the latest data will be read"
    )
    private Boolean restart;

    @FieldDoc(
            required = false,
            defaultValue = "true",
            sensitive = true,
            help = "Subscribe to the topic name of TDengine"
    )
    private String topic;

    @FieldDoc(
            required = true,
            defaultValue = "",
            sensitive = true,
            help = "The filter sql script, 此语句只能是 select 语句，只应查询原始数据，只能按时间正序查询数据"
    )
    private String sql;

    @FieldDoc(
            required = true,
            defaultValue = "",
            sensitive = true,
            help = ""
    )
    private String tableName;

    @FieldDoc(
            required = true,
            defaultValue = "",
            sensitive = true,
            help = ""
    )
    private String sTableName;

    public static TDengineSourceConfig load(String yamlFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(new File(yamlFile), TDengineSourceConfig.class);
    }

    public static TDengineSourceConfig load(Map<String, Object> map) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new ObjectMapper().writeValueAsString(map), TDengineSourceConfig.class);
    }

}