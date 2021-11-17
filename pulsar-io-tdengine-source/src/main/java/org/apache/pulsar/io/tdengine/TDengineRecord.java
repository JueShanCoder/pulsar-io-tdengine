package org.apache.pulsar.io.tdengine;

import lombok.Data;
import org.apache.pulsar.functions.api.Record;

import java.util.Optional;

@Data
public class TDengineRecord implements Record<byte[]> {

    private final Optional<String> key;
    private final byte[] value;
}