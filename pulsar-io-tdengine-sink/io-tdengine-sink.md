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
    "driver":"com.taosdata.jdbc.TSDBDriver",
    "jdbcUrl":"jdbc:TAOS://tdengine-2.0.18.0:6030",
    "username":"root",
    "password":"taosdata"
    }
    ```


* YAML

    ```
    configs:
        driver: "com.taosdata.jdbc.TSDBDriver",
        jdbcUrl: "jdbc:TAOS://tdengine-2.0.18.0:6030",
        username: "root",
        password: "taosdata"
    ```

## Usage

This example shows how to change the data of a TDengine table using the Pulsar TDengine connector.

> Tips: We use docker for simulation testing

1. Initialize docker cluster.
```shell
docker swarm init
```

2. Create new docker network
```shell
docker network create --attachable --driver overlay --subnet 10.1.0.0/16 --ip-range 10.1.0.0/24 tools
```

3. Start the TDengine container
```shell
docker run -d --name tdengine-2.0.18.0 --network tools tdengine/tdengine:2.0.18.0
```

4. Start the Pulsar container
```shell
 docker run -d --name pulsar-2.8.0 --network tools -m 3g -p 6650:6660 -p 6680:8080 --restart unless-stopped -v  pulsar-data:/pulsar/data -v pulsar-conf:/pulsar/conf apachepulsar/pulsar:2.8.0 ./bin/pulsar standalone
```

5. Enter the Pulsar container
```shell
docker exec -it pulsar-2.8.0 /bin/bash 
```

6. Start the Pulsar TDengine sink connector in local run mode using one of the following methods.

> Tips: Make sure the nar file is available at connectors/pulsar-io-tdengine-sink-2.8.0.nar

- Use the JSON configuration file as shown previously.
```shell
$ bin/pulsar-admin sinks localrun --name pulsar-tdengine-sink --tenant public --namespace default --inputs public/default/tdengine-source-topic --archive connectors/pulsar-io-tdengine-sink-2.8.0.nar --sink-config '{"driver":"com.taosdata.jdbc.TSDBDriver","jdbcUrl":"jdbc:TAOS://tdengine-2.0.18.0:6030/power_2","username":"root","password":"taosdata"}'
```
- Use the YAML configuration file as shown previously.

```shell
$ bin/pulsar-admin sinks localrun --sink-config-file io-tdengine-source-config.yaml
```

