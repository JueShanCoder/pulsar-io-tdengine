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
|`jdbcUrl`|true|None|The JDBC url of the database this connector connects to.
| `username` | true | None | Username used to connect to the database specified by `jdbcUrl`.|
| `password` | true | None | Password used to connect to the database specified by `jdbcUrl`.|
| `restart` | false | false | If restart is true, all data will be read, otherwise only the latest data will be read. |
| `sql` | true | None | This statement can only be a select statement, only the original data should be queried, and the data can only be queried in the positive order of time.|
| `database` | true | None | Database used to connect TDengine. |
| `tableName` | true | None | Table name used to connect TDengine. |
| `stableName` | false | None | Super table name used to connect TDengine. |

### Example 

Before using the TDengine connector, you can create a configuration file through one of the following methods.

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
# ?????????
taos> create table d1001 using meters tags ("Beijing.Chaoyang", 2);
taos> create table d1002 using meters tags ("Beijing.Haidian", 2);

```

7. Download TDengine-client from [TDengine official website](https://www.taosdata.com/cn/all-downloads/)
> Tips: Choose the TDengine version you need

8. Upload TDengine-client-2.0.18.0-Linux-x64.tar.gz to the Pulsar container and unzip.
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

9. Enter the Pulsar container
```shell
docker exec -it pulsar-2.8.0 /bin/bash 
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

11. Subscribe the topic sub-products for the tdengine-source-topic.
```shell
$ bin/pulsar-client consume -s "sub-products" public/default/tdengine-source-topic -n 0
```

12. Insert data in TDengine.
```shell
taos> insert into d1001 values("2020-08-15 12:00:00.000", 12, 220, 1),("2020-08-15 12:10:00.000", 12.3, 220, 2),("2020-08-15 12:20:00.000", 12.2, 220, 1);
```

- In the terminal window of subscribing topic, you can receive the following messages.
```shell
----- got message -----
key:[null], properties:[ACTION=INSERT, TARGET=power_2.meters.d1001], content:{"phase":"1","current":"12.0","groupid":"2","location":"Beijing.Chaoyang","ts":"2020-08-15 12:00:00.0","voltage":"220"}
----- got message -----
key:[null], properties:[ACTION=INSERT, TARGET=power_2.meters.d1001], content:{"phase":"2","current":"12.3","groupid":"2","location":"Beijing.Chaoyang","ts":"2020-08-15 12:10:00.0","voltage":"220"}
----- got message -----
key:[null], properties:[ACTION=INSERT, TARGET=power_2.meters.d1001], content:{"phase":"1","current":"12.2","groupid":"2","location":"Beijing.Chaoyang","ts":"2020-08-15 12:20:00.0","voltage":"220"}
```

