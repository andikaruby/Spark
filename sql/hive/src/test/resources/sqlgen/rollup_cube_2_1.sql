SELECT key, value, count(value) FROM parquet_t1 GROUP BY key, value WITH ROLLUP
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `key`, `gen_attr` AS `value`, `gen_attr` AS `count(value)` FROM (SELECT `gen_attr` AS `gen_attr`, `gen_attr` AS `gen_attr`, count(`gen_attr`) AS `gen_attr` FROM (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_0 GROUP BY `gen_attr`, `gen_attr` GROUPING SETS((`gen_attr`, `gen_attr`), (`gen_attr`), ())) AS gen_subquery_1
