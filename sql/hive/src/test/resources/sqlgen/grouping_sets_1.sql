SELECT count(*) AS cnt, key % 5 AS k1, key - 5 AS k2, grouping_id() AS k3
FROM (SELECT key, key % 2, key - 5 FROM parquet_t1) t GROUP BY key % 5, key - 5
GROUPING SETS (key % 5, key - 5)
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `cnt`, `gen_attr` AS `k1`, `gen_attr` AS `k2`, `gen_attr` AS `k3` FROM (SELECT count(1) AS `gen_attr`, (`gen_attr` % CAST(5 AS BIGINT)) AS `gen_attr`, (`gen_attr` - CAST(5 AS BIGINT)) AS `gen_attr`, grouping_id() AS `gen_attr` FROM (SELECT `gen_attr`, (`gen_attr` % CAST(2 AS BIGINT)) AS `gen_attr`, (`gen_attr` - CAST(5 AS BIGINT)) AS `gen_attr` FROM (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_0) AS t GROUP BY (`gen_attr` % CAST(5 AS BIGINT)), (`gen_attr` - CAST(5 AS BIGINT)) GROUPING SETS(((`gen_attr` % CAST(5 AS BIGINT))), ((`gen_attr` - CAST(5 AS BIGINT))))) AS gen_subquery_1
