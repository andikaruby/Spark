SELECT DISTINCT id FROM parquet_t0
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `id` FROM (SELECT DISTINCT `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`parquet_t0`) AS gen_subquery_0) AS parquet_t0