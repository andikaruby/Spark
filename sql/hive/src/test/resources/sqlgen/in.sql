-- This file is automatically generated by LogicalPlanToSQLSuite.
SELECT id FROM parquet_t0 WHERE id IN (1, 2, 3)
--------------------------------------------------------------------------------
SELECT `gen_attr_0` AS `id` FROM (SELECT `gen_attr_0` FROM (SELECT `id` AS `gen_attr_0` FROM `default`.`parquet_t0`) AS gen_subquery_0 WHERE (CAST(`gen_attr_0` AS BIGINT) IN (CAST(1 AS BIGINT), CAST(2 AS BIGINT), CAST(3 AS BIGINT)))) AS parquet_t0
