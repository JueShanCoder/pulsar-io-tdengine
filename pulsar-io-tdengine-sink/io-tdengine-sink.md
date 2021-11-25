---
id: version-2.8.2-io-tdengine-sink
title: Tdengine sink connector
sidebar_label: Tdengine sink connector
original_id: io-tdengine-sink
---

The TDengine sink connector pulls messages from Pulsar topics to TDengine clusters.

## Configuration

The configuration of the TDengine sink connector has the following properties.

### Property

| Name | Type|Required | Default | Description
|------|----------|----------|---------|-------------|
| `username` | String|true | None | Username used to connect to the database specified by `jdbcUrl`.|
| `password` | String|true| None| Password used to connect to the database specified by jdbcUrl.|
| `driver` | String|true| None| The key name of the Cassandra column family. <br><br>The column is used for storing Pulsar message keys. <br><br>If a Pulsar message doesn't have any key associated, the message value is used as the key. |
| `jdbcUrl` | String|true| None| TThe JDBC url of the database this connector connects to.|
| `timezone` | String|true| GMT+8 | Default timezone of the JVM instance. |

### Example

Before using the TDengine sink connector, you need to create a configuration file through one of the following methods.

* JSON

    ```json
    {
        "roots": "localhost:9042",
        "keyspace": "pulsar_test_keyspace",
        "columnFamily": "pulsar_test_table",
        "keyname": "key",
        "columnName": "col"
    }
    ```


* YAML

    ```
    configs:
        roots: "localhost:9042"
        keyspace: "pulsar_test_keyspace"
        columnFamily: "pulsar_test_table"
        keyname: "key"
        columnName: "col"
    ```

## Usage
