-- This file is automatically generated by LogicalPlanToSQLSuite.
SELECT /*+ MAPJOIN(parquet_t0, parquet_t1) */ * FROM parquet_t0, parquet_t1
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `id`, `gen_attr` AS `key`, `gen_attr` AS `value` FROM (SELECT /*+ MAPJOIN(parquet_t0, parquet_t1) */ `gen_attr`, `gen_attr`, `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`parquet_t0`) AS gen_subquery_0 INNER JOIN (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_1) AS gen_subquery_2
