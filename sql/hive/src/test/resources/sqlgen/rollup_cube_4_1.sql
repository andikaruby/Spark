SELECT count(*) as cnt, key % 5 as k1, key - 5 as k2, grouping_id() FROM parquet_t1
GROUP BY key % 5, key - 5 WITH ROLLUP
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `cnt`, `gen_attr` AS `k1`, `gen_attr` AS `k2`, `gen_attr` AS `grouping_id()` FROM (SELECT count(1) AS `gen_attr`, (`gen_attr` % CAST(5 AS BIGINT)) AS `gen_attr`, (`gen_attr` - CAST(5 AS BIGINT)) AS `gen_attr`, grouping_id() AS `gen_attr` FROM (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_0 GROUP BY (`gen_attr` % CAST(5 AS BIGINT)), (`gen_attr` - CAST(5 AS BIGINT)) GROUPING SETS(((`gen_attr` % CAST(5 AS BIGINT)), (`gen_attr` - CAST(5 AS BIGINT))), ((`gen_attr` % CAST(5 AS BIGINT))), ())) AS gen_subquery_1
