-- This file is automatically generated by LogicalPlanToSQLSuite.
SELECT COUNT(value) FROM parquet_t1 GROUP BY key HAVING MAX(key) > 0
--------------------------------------------------------------------------------
SELECT `gen_attr_0` AS `count(value)` FROM (SELECT `gen_attr_0` FROM (SELECT count(`gen_attr_3`) AS `gen_attr_0`, max(`gen_attr_2`) AS `gen_attr_1` FROM (SELECT `key` AS `gen_attr_2`, `value` AS `gen_attr_3` FROM `default`.`parquet_t1`) AS gen_subquery_0 GROUP BY `gen_attr_2` HAVING (`gen_attr_1` > CAST(0 AS BIGINT))) AS gen_subquery_1) AS gen_subquery_2
