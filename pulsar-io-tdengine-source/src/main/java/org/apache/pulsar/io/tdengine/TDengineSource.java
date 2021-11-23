package org.apache.pulsar.io.tdengine;

import com.alibaba.fastjson.JSON;
import org.apache.pulsar.io.core.annotations.Connector;
import org.apache.pulsar.io.core.annotations.IOType;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Connector(
        name = "TDengineSource",
        type = IOType.SOURCE,
        help = "A TDengine source connector for Pulsar ",
        configClass = TDengineSourceConfig.class)
public class TDengineSource extends TDengineAbstractSource<byte[]>{

    @Override
    public byte[] extractValue(Map<String,Object> columnMap) {
        return JSON.toJSONString(columnMap).getBytes(StandardCharsets.UTF_8);
    }

}