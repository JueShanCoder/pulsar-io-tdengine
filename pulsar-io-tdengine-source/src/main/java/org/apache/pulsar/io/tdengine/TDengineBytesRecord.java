package org.apache.pulsar.io.tdengine;

import java.util.HashMap;
import java.util.Map;

public class TDengineBytesRecord extends TDengineAbstractRecord<byte[]>{

    private final HashMap<String, String> userProperties = new HashMap<> ();

    @Override
    public Map<String, String> getProperties() {
        return userProperties;
    }

    public void setProperties(String database, String stable, String table) {
        String TARGET = "TARGET";
        String ACTION = "ACTION";
        String INSERT = "INSERT";
        if (stable != null)
            userProperties.put(TARGET,database + "." + stable + "." + table);
        else
            userProperties.put(TARGET,database + "." + table);
        userProperties.put(ACTION, INSERT);
    }
}