SELECT id FROM parquet_t0 UNION ALL SELECT CAST(id AS INT) AS id FROM parquet_t0
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `id` FROM ((SELECT `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`parquet_t0`) AS gen_subquery_0) UNION ALL (SELECT CAST(CAST(`gen_attr` AS INT) AS BIGINT) AS `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`parquet_t0`) AS gen_subquery_1)) AS parquet_t0
