-- This file is automatically generated by LogicalPlanToSQLSuite.
SELECT /*+ MAPJOIN(parquet_t0) */ * FROM parquet_t0, parquet_t1
--------------------------------------------------------------------------------
SELECT `gen_attr_0` AS `id`, `gen_attr_1` AS `key`, `gen_attr_2` AS `value` FROM (SELECT /*+ MAPJOIN(parquet_t0) */ `gen_attr_0`, `gen_attr_1`, `gen_attr_2` FROM (SELECT `id` AS `gen_attr_0` FROM `default`.`parquet_t0`) AS gen_subquery_0 INNER JOIN (SELECT `key` AS `gen_attr_1`, `value` AS `gen_attr_2` FROM `default`.`parquet_t1`) AS gen_subquery_1) AS gen_subquery_2
