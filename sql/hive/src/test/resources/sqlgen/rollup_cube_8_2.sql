SELECT hkey AS k1, value - 5 AS k2, hash(grouping_id()) AS hgid
FROM (SELECT hash(key) as hkey, key as value FROM parquet_t1) t GROUP BY hkey, value-5
WITH CUBE
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `k1`, `gen_attr` AS `k2`, `gen_attr` AS `hgid` FROM (SELECT `gen_attr` AS `gen_attr`, (`gen_attr` - CAST(5 AS BIGINT)) AS `gen_attr`, hash(grouping_id()) AS `gen_attr` FROM (SELECT hash(`gen_attr`) AS `gen_attr`, `gen_attr` AS `gen_attr` FROM (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_0) AS t GROUP BY `gen_attr`, (`gen_attr` - CAST(5 AS BIGINT)) GROUPING SETS((`gen_attr`, (`gen_attr` - CAST(5 AS BIGINT))), (`gen_attr`), ((`gen_attr` - CAST(5 AS BIGINT))), ())) AS gen_subquery_1
