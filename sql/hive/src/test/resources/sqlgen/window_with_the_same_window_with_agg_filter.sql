SELECT key, value,
DENSE_RANK() OVER (DISTRIBUTE BY key SORT BY key, value) AS dr,
COUNT(key) OVER(DISTRIBUTE BY key SORT BY key, value) AS ca
FROM parquet_t1
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `key`, `gen_attr` AS `value`, `gen_attr` AS `dr`, `gen_attr` AS `ca` FROM (SELECT `gen_attr`, `gen_attr`, `gen_attr`, `gen_attr` FROM (SELECT gen_subquery_1.`gen_attr`, gen_subquery_1.`gen_attr`, DENSE_RANK() OVER (PARTITION BY `gen_attr` ORDER BY `gen_attr` ASC, `gen_attr` ASC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS `gen_attr`, count(`gen_attr`) OVER (PARTITION BY `gen_attr` ORDER BY `gen_attr` ASC, `gen_attr` ASC RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS `gen_attr` FROM (SELECT `gen_attr`, `gen_attr` FROM (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_0) AS gen_subquery_1) AS gen_subquery_2) AS parquet_t1