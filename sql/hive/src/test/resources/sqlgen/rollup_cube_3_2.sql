SELECT key, count(value), grouping_id() FROM parquet_t1 GROUP BY key, value WITH CUBE
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `key`, `gen_attr` AS `count(value)`, `gen_attr` AS `grouping_id()` FROM (SELECT `gen_attr` AS `gen_attr`, count(`gen_attr`) AS `gen_attr`, grouping_id() AS `gen_attr` FROM (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_0 GROUP BY `gen_attr`, `gen_attr` GROUPING SETS((`gen_attr`, `gen_attr`), (`gen_attr`), (`gen_attr`), ())) AS gen_subquery_1
