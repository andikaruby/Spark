SELECT COUNT(value) FROM parquet_t1 GROUP BY key ORDER BY key, MAX(key)
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `count(value)` FROM (SELECT `gen_attr` FROM (SELECT count(`gen_attr`) AS `gen_attr`, `gen_attr` AS `gen_attr`, max(`gen_attr`) AS `gen_attr` FROM (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_0 GROUP BY `gen_attr` ORDER BY `gen_attr` ASC, `gen_attr` ASC) AS gen_subquery_1) AS gen_subquery_2
