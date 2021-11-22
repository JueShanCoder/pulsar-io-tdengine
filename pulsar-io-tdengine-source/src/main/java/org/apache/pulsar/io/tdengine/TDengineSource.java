package org.apache.pulsar.io.tdengine;

import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.schema.GenericRecord;
import org.apache.pulsar.client.api.schema.GenericRecordBuilder;
import org.apache.pulsar.client.api.schema.SchemaBuilder;
import org.apache.pulsar.common.schema.SchemaType;
import org.apache.pulsar.io.core.annotations.Connector;
import org.apache.pulsar.io.core.annotations.IOType;

import java.util.Map;

@Connector(
        name = "TDengineSource",
        type = IOType.SOURCE,
        help = "A TDengine source connector for Pulsar ",
        configClass = TDengineSourceConfig.class)
public class TDengineSource extends TDengineAbstractSource<GenericRecord>{

    @Override
    public GenericRecord extractValue(Map<String,Object> columnMap) {
        recordSchemaBuilder = SchemaBuilder.record("mybean");
        columnMap.forEach((k,v) -> {
            this.recordSchemaBuilder.field(k).type(SchemaType.STRING);
        });
        schema = Schema.generic(this.recordSchemaBuilder.build(SchemaType.AVRO));
        GenericRecordBuilder genericRecordBuilder = schema.newRecordBuilder();
        columnMap.forEach(genericRecordBuilder::set);
        return genericRecordBuilder.build();
    }

}