---
id: version-2.8.2-io-tdengine-source
title: TDengine source connector
sidebar_label: TDengine source connector
original_id: io-tdengine-source
---


The TDengine source connector pulls messages from TDengine to Pulsar topics.

## Configuration
The configuration of TDengine source connector has the following properties.

### Property

| Name | Required | Default | Description |
|------|----------|---------|-------------|
| `charset` | false | UTF-8 | TSDB Driver properties key charset.|
| `timezone` | false | UTC-8 | TSDB Driver properties key timezone. |
|`jdbcUrl`|true|None|The JDBC url of the database this connector connects to.
| `username` | true | None | Username used to connect to the database specified by `jdbcUrl`.|
| `password` | true | None | Password used to connect to the database specified by `jdbcUrl`.|
| `restart` | false | false | If restart is true, all data will be read, otherwise only the latest data will be read. |
| `sql` | true | None | This statement can only be a select statement, only the original data should be queried, and the data can only be queried in the positive order of time.|
| `database` | true | None | Database used to connect TDengine. |
| `tableName` | true | None | Table name used to connect TDengine. |
| `sTableName` | false | None | Super table name used to connect TDengine. |

### Example 

Before using the TDengine connector, you can create a configuration file through one of the following methods.

* JSON

    ```json
    {
        "zkServers": "127.0.0.1:2181",
        "batchSize": "5120",
        "destination": "example",
        "username": "",
        "password": "",
        "cluster": false,
        "singleHostname": "127.0.0.1",
        "singlePort": "11111",
    }
    ```

* YAML

  You can create a YAML file and copy the [contents](https://github.com/apache/pulsar/blob/master/pulsar-io/canal/src/main/resources/canal-mysql-source-config.yaml) below to your YAML file.

    ```yaml
    configs:
        zkServers: "127.0.0.1:2181"
        batchSize: 5120
        destination: "example"
        username: ""
        password: ""
        cluster: false
        singleHostname: "127.0.0.1"
        singlePort: 11111
    ```
  
## Usage