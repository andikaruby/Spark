SELECT val, id FROM parquet_t3 LATERAL VIEW EXPLODE(arr) exp AS val
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `val`, `gen_attr` AS `id` FROM (SELECT `gen_attr`, `gen_attr` FROM (SELECT `arr` AS `gen_attr`, `arr2` AS `gen_attr`, `json` AS `gen_attr`, `id` AS `gen_attr` FROM `default`.`parquet_t3`) AS gen_subquery_0 LATERAL VIEW explode(`gen_attr`) gen_subquery_2 AS `gen_attr`) AS gen_subquery_1
