package org.apache.pulsar.io.tdengine;

import lombok.Data;
import org.apache.pulsar.functions.api.Record;

@Data
public abstract class TDengineAbstractRecord<V> implements Record<V>  {

    private V record;

    @Override
    public V getValue() {
        return record;
    }

}