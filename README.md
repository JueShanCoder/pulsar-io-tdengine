
The TDengine connector connects Pulsar and TDengine.

## Configuration
The configuration of TDengine source connector has the following properties.

### Property

#### TDengine Source
| Name | Required | Default | Description |
|------|----------|---------|-------------|
|`jdbcUrl`|true|None|The JDBC url of the database this connector connects to.
| `username` | true | None | Username used to connect to the database specified by `jdbcUrl`.|
| `password` | true | None | Password used to connect to the database specified by `jdbcUrl`.|
| `restart` | false | false | If restart is true, all data will be read, otherwise only the latest data will be read. |
| `sql` | true | None | This statement can only be a select statement, only the original data should be queried, and the data can only be queried in the positive order of time.|
| `database` | true | None | Database used to connect TDengine. |
| `tableName` | true | None | Table name used to connect TDengine. |
| `stableName` | false | None | Super table name used to connect TDengine. |

#### TDengine Sink

| Name | Type|Required | Default | Description
|------|----------|----------|---------|-------------|
| `username` | String|true | None | Username used to connect to the database specified by `jdbcUrl`.|
| `password` | String|true| None| Password used to connect to the database specified by jdbcUrl.|
| `driver` | String|true| None| The key name of the Cassandra column family. <br><br>The column is used for storing Pulsar message keys. <br><br>If a Pulsar message doesn't have any key associated, the message value is used as the key. |
| `jdbcUrl` | String|true| None| TThe JDBC url of the database this connector connects to.|
| `timezone` | String|true| GMT+8 | Default timezone of the JVM instance. |

### Example

Before using the TDengine connector, you can create a configuration file through one of the following methods.

#### TDengine Source Configuration
* JSON

    ```json
    {
    "jdbcUrl":"jdbc:TAOS://tdengine-2.0.18.0:6030/power",
    "username":"root",
    "password":"taosdata",
    "sql":"select * from meters",
    "database": "power_2",
    "tableName": "d1001",
    "stableName": "meters"
    }
    ```

* YAML

  You can create a YAML file and copy the [contents](https://github.com/apache/pulsar/blob/master/pulsar-io/canal/src/main/resources/canal-mysql-source-config.yaml) below to your YAML file.

    ```yaml
    configs:
        jdbcUrl: "jdbc:TAOS://tdengine-2.0.18.0:6030/power"
        username: "root"
        password: "taosdata"
        sql: "select * from meters"
        database: "power_2"
        tableName: "d1001"
        stableName: "meters"
    ```
#### TDengine Sink Configuration

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

> Tips: We use docker for simulation testing. If you already have docker's TDengine and Pulsar containers,you can skip step 1,2,3,4

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

5. Enter the TDengine container
```shell
docker exec -it tdengine-2.0.18.0 /bin/bash
```

6. Initialize the data.
```shell
$ taos
```

```sql
taos> create database power;
taos> use power;
taos> create table meters(ts timestamp, current float, voltage int, phase int) tags(location binary(64), groupId int);
taos> create table d1001 using meters tags ("Beijing.Chaoyang", 2);
taos> create table d1002 using meters tags ("Beijing.Haidian", 2);


taos> create database power_2;
taos> use power_2;
taos> create table meters(ts timestamp, current float, voltage int, phase int) tags(location binary(64), groupId int);
taos> create table d1001 using meters tags ("Beijing.Chaoyang", 2);
taos> create table d1002 using meters tags ("Beijing.Haidian", 2);

```

7. Download TDengine-client from [TDengine official website](https://www.taosdata.com/cn/all-downloads/)
> Tips: Choose the TDengine version you need

8. Enter the Pulsar container
```shell
docker exec -it pulsar-2.8.0 /bin/bash 
```

9. Upload TDengine-client-2.0.18.0-Linux-x64.tar.gz to the Pulsar container and unzip.
```shell
$ tar -zxvf TDengine-client-2.0.18.0-Linux-x64.tar.gz
```
```shell
$ cd TDengine-client-2.0.18.0
```
```shell
$ ./install_client.sh
$  Start to install TDengine client...
 
$  TDengine client is installed successfully!
```

10. Start the Pulsar TDengine source connector in local run mode using one of the following methods.

> Tips: Make sure the nar file is available at connectors/pulsar-io-tdengine-source-2.8.0.nar

- Use the JSON configuration file as shown previously.
```shell
$ bin/pulsar-admin source localrun --destination-topic-name tdengine-source-topic --tenant public --namespace default --name pulsar-tdengine-source --archive connectors/pulsar-io-tdengine-source-2.8.0.nar --parallelism 1 --source-config '{ "jdbcUrl":"jdbc:TAOS://tdengine-2.0.18.0:6030/power", "username":"root", "password":"taosdata", "sql":"select * from meters", "database": "power_2", "tableName": "d1001", "stableName": "meters" }'
```
- Use the YAML configuration file as shown previously.

```shell
$ bin/pulsar-admin source localrun --source-config-file io-tdengine-source-config.yaml
```

11. Start the Pulsar TDengine sink connector in local run mode using one of the following methods.

> Tips: Make sure the nar file is available at connectors/pulsar-io-tdengine-sink-2.8.0.nar

- Use the JSON configuration file as shown previously.
```shell
$ bin/pulsar-admin sinks localrun --name pulsar-tdengine-sink --tenant public --namespace default --inputs public/default/tdengine-source-topic --archive connectors/pulsar-io-tdengine-sink-2.8.0.nar --sink-config '{"driver":"com.taosdata.jdbc.TSDBDriver","jdbcUrl":"jdbc:TAOS://tdengine-2.0.18.0:6030/power_2","username":"root","password":"taosdata"}'
```

- Use the YAML configuration file as shown previously.
```shell
$ bin/pulsar-admin sinks localrun --sink-config-file io-tdengine-sink-config.yaml
```

12. Insert data in TDengine.
```shell
taos> use power;
taos> insert into d1001 values("2020-08-15 12:00:00.000", 12, 220, 1),("2020-08-15 12:10:00.000", 12.3, 220, 2),("2020-08-15 12:20:00.000", 12.2, 220, 1);

taos> use power_2;
taos> select * from meters;
taos> select * from meters;

           ts            |       current        |   voltage   |    phase    |            location            |   groupid   |
============================================================================================================================
 2020-08-15 12:00:00.000 |             12.00000 |         220 |           1 | Beijing.Chaoyang               |           2 |
 2020-08-15 12:10:00.000 |             12.30000 |         220 |           2 | Beijing.Chaoyang               |           2 |
 2020-08-15 12:20:00.000 |             12.20000 |         220 |           1 | Beijing.Chaoyang               |           2 |
Query OK, 3 row(s) in set (0.009826s)

```

