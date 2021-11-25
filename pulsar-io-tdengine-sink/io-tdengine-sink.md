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
| `password` | String|true| None| The key space used for writing pulsar messages. <br><br>**Note: `keyspace` should be created prior to a Cassandra sink.**|
| `driver` | String|true| None| The key name of the Cassandra column family. <br><br>The column is used for storing Pulsar message keys. <br><br>If a Pulsar message doesn't have any key associated, the message value is used as the key. |
| `jdbcUrl` | String|true| None| The Cassandra column family name.<br><br>**Note: `columnFamily` should be created prior to a Cassandra sink.**|
| `timezone` | String|true| GMT+8 | The column name of the Cassandra column family.<br><br> The column is used for storing Pulsar message values. |
