-- This file is automatically generated by LogicalPlanToSQLSuite.
SELECT EXPLODE(arr) AS val, id
FROM parquet_t3
WHERE id > 2
ORDER BY val, id
LIMIT 5
--------------------------------------------------------------------------------
SELECT `gen_attr_0` AS `val`, `gen_attr_1` AS `id` FROM ((SELECT `gen_attr_0`, `gen_attr_1` FROM (SELECT gen_subquery_0.`gen_attr_2`, gen_subquery_0.`gen_attr_3`, gen_subquery_0.`gen_attr_4`, gen_subquery_0.`gen_attr_1` FROM (SELECT `arr` AS `gen_attr_2`, `arr2` AS `gen_attr_3`, `json` AS `gen_attr_4`, `id` AS `gen_attr_1` FROM `default`.`parquet_t3`) AS gen_subquery_0 WHERE (`gen_attr_1` > CAST(2 AS BIGINT))) AS gen_subquery_1 LATERAL VIEW explode(`gen_attr_2`) gen_subquery_2 AS `gen_attr_0` ORDER BY `gen_attr_0` ASC NULLS FIRST, `gen_attr_1` ASC NULLS FIRST LIMIT 5)) AS parquet_t3
