SELECT id FROM parquet_t0
UNION ALL SELECT id FROM parquet_t0
UNION ALL SELECT id FROM parquet_t0
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `id` FROM ((SELECT `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`parquet_t0`) AS gen_subquery_0) UNION ALL (SELECT `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`parquet_t0`) AS gen_subquery_1) UNION ALL (SELECT `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`parquet_t0`) AS gen_subquery_2)) AS parquet_t0