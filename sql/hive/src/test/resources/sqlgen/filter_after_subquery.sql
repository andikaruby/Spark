SELECT a FROM (SELECT key + 1 AS a FROM parquet_t1) t WHERE a > 5
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `a` FROM (SELECT `gen_attr` FROM (SELECT (`gen_attr` + CAST(1 AS BIGINT)) AS `gen_attr` FROM (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_0) AS t WHERE (`gen_attr` > CAST(5 AS BIGINT))) AS t
