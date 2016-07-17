SELECT t.key - 5, cnt, SUM(cnt)
FROM (SELECT x.key, COUNT(*) as cnt
FROM parquet_t1 x JOIN parquet_t1 y ON x.key = y.key GROUP BY x.key) t
GROUP BY cnt, t.key - 5
WITH CUBE
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `(key - CAST(5 AS BIGINT))`, `gen_attr` AS `cnt`, `gen_attr` AS `sum(cnt)` FROM (SELECT (`gen_attr` - CAST(5 AS BIGINT)) AS `gen_attr`, `gen_attr` AS `gen_attr`, sum(`gen_attr`) AS `gen_attr` FROM (SELECT `gen_attr`, count(1) AS `gen_attr` FROM (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_0 INNER JOIN (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_1 ON (`gen_attr` = `gen_attr`) GROUP BY `gen_attr`) AS t GROUP BY `gen_attr`, (`gen_attr` - CAST(5 AS BIGINT)) GROUPING SETS((`gen_attr`, (`gen_attr` - CAST(5 AS BIGINT))), (`gen_attr`), ((`gen_attr` - CAST(5 AS BIGINT))), ())) AS gen_subquery_2