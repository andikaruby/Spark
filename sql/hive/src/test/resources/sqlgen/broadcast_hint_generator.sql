-- This file is automatically generated by LogicalPlanToSQLSuite.
SELECT * FROM (SELECT /*+ MAPJOIN(parquet_t0) */ EXPLODE(ARRAY(1,2,3)) FROM parquet_t0) T
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `col` FROM (SELECT `gen_attr` FROM (SELECT /*+ MAPJOIN(parquet_t0) */ `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`parquet_t0`) AS gen_subquery_0 LATERAL VIEW explode(array(1, 2, 3)) gen_subquery_1 AS `gen_attr`) AS T) AS T
