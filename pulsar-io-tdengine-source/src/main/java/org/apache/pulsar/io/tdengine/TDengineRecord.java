package org.apache.pulsar.io.tdengine;

import lombok.Data;
import org.apache.pulsar.functions.api.Record;

import java.util.Collections;
import java.util.Map;

@Data
public class TDengineRecord<V> implements Record<V>  {

    private V record;

    @Override
    public V getValue() {
        return record;
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }
}