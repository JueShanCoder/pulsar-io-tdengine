# Pulsar IO JDBC

## Sink

``` sh
$ ./bin/pulsar-admin sinks localrun --name local-jdbc-sink --retain-ordering --processing-guarantees EFFECTIVELY_ONCE --parallelism 1 --tenant public --namespace practices --inputs public/practices/local-jdbc --archive connectors/pulsar-io-jdbc-1.0.0-SNAPSHOT.nar --sink-config '{"driver":"com.mysql.cj.jdbc.Driver","jdbcUrl":"jdbc:mysql://mysql:3306?disableMariaDbDriver","username":"","password":""}'
```

## Actions

- `INSERT`
- `UPSERT`
- `UPDATE`
- `DELETE`
- `SCHEMA`

## Data Types

| JDBC Type                      | Java Type                 | JSON Type | Format        | Sample / Range                                          | H2   | MySQL           | MariaDB         | PostgreSQL      |
| ------------------------------ | ------------------------- | --------- | ------------- | ------------------------------------------------------- | ---- | --------------- | --------------- | --------------- |
| `BIT(1)`                       | `java.lang.Boolean`       | `boolean` |               | `true`, `false`                                         | ✓    | ✓               | ✓               | ✗[^dt_pg_bit1]  |
| `BOOLEAN`                      | `java.lang.Boolean`       | `boolean` |               | `true`, `false`                                         | ✓    | ✓               | ✓               | ✓               |
| `TINYINT`                      | `java.lang.Byte`          | `number`  |               | `-128`, `127`                                           | ✓    | ✓               | ✓               | ✓               |
| `TINYNT UNSIGNED`              | `java.lang.Short`         | `number`  |               | `0`, `255`                                              | ✗    | ✓               | ✓               | ✗               |
| `SMALLINT`                     | `java.lang.Short`         | `number`  |               | `-32768`, `32767`                                       | ✓    | ✓               | ✓               | ✓               |
| `SMALLINT UNSIGNED`            | `java.lang.Short`         | `number`  |               | `0`, `65535`                                            | ✗    | ✓               | ✓               | ✗               |
| `INTEGER`                      | `java.lang.Integer`       | `number`  |               | `-2147483648`, `2147483647`                             | ✓    | ✓               | ✓               | ✓               |
| `INTEGER UNSIGNED`             | `java.lang.Long`          | `number`  |               | `0`, `4294967295`                                       | ✗    | ✓               | ✓               | ✗               |
| `BIGINT`                       | `java.lang.Long`          | `number`  |               | `-9223372036854775808`, `9223372036854775807`           | ✓    | ✓               | ✓               | ✓               |
| `BIGINT UNSIGNED`              | `java.math.BigInteger`    | `number`  |               | `0`, `18446744073709551615`                             | ✗    | ✓               | ✓               | ✗               |
| `FLOAT`                        | `java.lang.Float`         | `number`  |               | `1.4e-45`, `3.4028235e+38`                              | ✓    | ✗[^dt_m*_float] | ✗[^dt_m*_float] | ✓               |
| `REAL`[^dt_*_real]             | `java.lang.Double`        | `number`  |               | `2.2250738585072014E-308`, `1.7976931348623157e+308`    | ✓    | ✓               | ✓               | ✓               |
| `DOUBLE`                       | `java.lang.Double`        | `number`  |               | `2.2250738585072014E-308`, `1.7976931348623157e+308`    | ✓    | ✓               | ✓               | ✓               |
| `NUMERIC(m, d)`[^dt_*_decimal] | `java.math.BigDecimal`    | `number`  |               | `-2.7182818284590452354`, `3.14159265358979323846`      | ✓    | ✓               | ✓               | ✓               |
| `DECIMAL(m, d)`                | `java.math.BigDecimal`    | `number`  |               | `-2.7182818284590452354`, `3.14159265358979323846`      | ✓    | ✓               | ✓               | ✓               |
| `CHAR(n)`                      | `java.lang.String`        | `string`  |               | `"Hello, World."`                                       | ✓    | ✓               | ✓               | ✓               |
| `VARCHAR(n)`                   | `java.lang.String`        | `string`  |               | `"Hello, World."`                                       | ✓    | ✓               | ✓               | ✓               |
| `LONGVARCHAR(n)`               | `java.lang.String`        | `string`  |               | `"Hello, World."`                                       | ✓    | ✓               | ✓               | ✗[^dt_pg_text]  |
| `BINARY(n)`                    | `java.lang.Byte[]`        | `string`  | Base64Encoded | `"SGVsbG8sIFdvcmxkLg=="`                                | ✓    | ✓               | ✓               | ✓               |
| `VARBINARY(n)`                 | `java.lang.Byte[]`        | `string`  | Base64Encoded | `"SGVsbG8sIFdvcmxkLg=="`                                | ✓    | ✓               | ✓               | ✗[^dt_pg_bytea] |
| `LONGVARBINARY(n)`             | `java.lang.Byte[]`        | `string`  | Base64Encoded | `"SGVsbG8sIFdvcmxkLg=="`                                | ✓    | ✓               | ✓               | ✗[^dt_pg_bytea] |
| `DATE`                         | `java.time.LocalDate`     | `string`  | ISO-8601      | `"2020-04-02"`                                          | ✓    | ✓               | ✓               | ✓               |
| `TIME`                         | `java.time.LocalTime`     | `string`  | ISO-8601      | `"09:17:46"`, `"10:05:33.123456"`                       | ✓    | ✓               | ✓               | ✓               |
| `TIMESTAMP`                    | `java.time.LocalDateTime` | `string`  | ISO-8601      | `"2020-04-02T09:17:46"`, `"2020-04-02T10:05:33.123456"` | ✓    | ✓               | ✓               | ✓               |

[^dt_pg_bit1]: PostgreSQL中`BIT(1)`字段类型会被映射为`JDBCType.BIT`，用于表示定长位数二进制数值。

[^dt_*_real]: `REAL`类型是`DOUBLE`的别名。

[^dt_m*_float]: MySQL或MariaDB中`FLOAT`字段类型取值范围与`java.lang.Float`不兼容，不推荐使用。

[^dt_*_decimal]: `DECIMAL`类型是`NUMERIC`的别名。

[^dt_pg_text]: PostgreSQL中`TEXT`字段类型会被映射为`JDBCType.VARCHAR`，用于存储无限长度变长文本数据

[^dt_pg_bytea]: PostgreSQL中`BYTEA`字段类型会被映射为`JDBCType.VARBINARY`，用于存储无限长度变长二进制数据。

### References:

http://www.h2database.com/html/datatypes.html

https://dev.mysql.com/doc/refman/5.7/en/data-types.html

https://mariadb.com/kb/en/data-types/

https://www.postgresql.org/docs/12/datatype.html