-- This file is automatically generated by LogicalPlanToSQLSuite.
SELECT /*+ MAPJOIN(parquet_t0) */ * FROM parquet_t0 WHERE id < 10
--------------------------------------------------------------------------------
SELECT `gen_attr_0` AS `id` FROM (SELECT /*+ MAPJOIN(parquet_t0) */ `gen_attr_0` FROM (SELECT `id` AS `gen_attr_0` FROM `default`.`parquet_t0`) AS gen_subquery_0 WHERE (`gen_attr_0` < CAST(10 AS BIGINT))) AS parquet_t0
