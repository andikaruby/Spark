SELECT COUNT(a.value), b.KEY, a.KEY
FROM parquet_t1 a, parquet_t1 b
GROUP BY a.KEY, b.KEY
HAVING MAX(a.KEY) > 0
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `count(value)`, `gen_attr` AS `KEY`, `gen_attr` AS `KEY` FROM (SELECT `gen_attr`, `gen_attr`, `gen_attr` FROM (SELECT count(`gen_attr`) AS `gen_attr`, `gen_attr`, `gen_attr`, max(`gen_attr`) AS `gen_attr` FROM (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_0 INNER JOIN (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_1 GROUP BY `gen_attr`, `gen_attr` HAVING (`gen_attr` > CAST(0 AS BIGINT))) AS gen_subquery_2) AS gen_subquery_3
