SELECT TRANSFORM (key, value)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe'
USING 'cat' AS (tKey, tValue)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe'
FROM parquet_t1
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `tKey`, `gen_attr` AS `tValue` FROM (SELECT TRANSFORM (`gen_attr`, `gen_attr`) ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe' USING 'cat' AS (`gen_attr` string, `gen_attr` string) ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe' FROM (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_0) AS gen_subquery_1