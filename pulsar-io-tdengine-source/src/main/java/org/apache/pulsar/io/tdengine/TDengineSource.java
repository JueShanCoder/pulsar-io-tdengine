package org.apache.pulsar.io.tdengine;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.schema.GenericRecord;
import org.apache.pulsar.client.api.schema.GenericRecordBuilder;
import org.apache.pulsar.client.api.schema.GenericSchema;
import org.apache.pulsar.common.schema.SchemaInfo;
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
//        SchemaInfo schemaInfo = Schema.AUTO_PRODUCE_BYTES().getSchemaInfo();
//        GenericSchema generic = Schema.generic(schemaInfo);
//        GenericRecordBuilder genericRecordBuilder = generic.newRecordBuilder();
//        columnMap.forEach(genericRecordBuilder::set);
//        return genericRecordBuilder.build();
        return JSON.toJSONString(columnMap, SerializerFeature.WriteMapNullValue).getBytes(StandardCharsets.UTF_8);
    }

}