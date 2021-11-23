package org.apache.pulsar.io.tdengine;

import lombok.Data;
import org.apache.pulsar.functions.api.Record;

import java.util.HashMap;
import java.util.Map;

@Data
public class TDengineRecord<V> implements Record<V>  {

    private V record;

    private final HashMap<String, String> userProperties = new HashMap<> ();

    @Override
    public V getValue() {
        return record;
    }

    @Override
    public Map<String, String> getProperties() {
        return userProperties;
    }
}