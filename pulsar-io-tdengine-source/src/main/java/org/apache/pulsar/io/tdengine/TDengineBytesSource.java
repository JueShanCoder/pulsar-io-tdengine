package org.apache.pulsar.io.tdengine;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taosdata.jdbc.TSDBResultSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.io.core.annotations.Connector;
import org.apache.pulsar.io.core.annotations.IOType;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Connector(
        name = "TDengineBytesSource",
        type = IOType.SOURCE,
        help = "A TDengine source connector for Pulsar ",
        configClass = TDengineSourceConfig.class)
@Slf4j
public class TDengineBytesSource extends TDengineAbstractSource<byte[]>{

    @Override
    void process() {
        {
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
                        TDengineBytesRecord tDengineRecord = new TDengineBytesRecord();
                        tDengineRecord.setRecord(extractValue(columnMap));
                        tDengineRecord.setProperties(
                                tDengineSourceConfig.getDatabase(),
                                tDengineSourceConfig.getSTableName(),
                                tDengineSourceConfig.getTableName()
                        );
                        log.info("TDengineAbstractRecord got message {}",new ObjectMapper().writeValueAsString(tDengineRecord));
                        consume(tDengineRecord);
                    }
                }
            } catch (Exception e) {
                log.error("process error!", e);
            } finally {
                try {
                    if (null != subscribe)
                        subscribe.close(true);
                    if (connection != null)
                        connection.close();
                } catch (SQLException e) {
                    log.error("Failed to close connection or subscribe.");
                }
            }
        }
    }

    @Override
    public byte[] extractValue(Map<String,Object> columnMap) {
        return JSON.toJSONString(columnMap).getBytes(StandardCharsets.UTF_8);
    }
}