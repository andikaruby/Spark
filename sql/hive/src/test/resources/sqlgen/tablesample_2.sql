SELECT * FROM parquet_t0 TABLESAMPLE(100 PERCENT)
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `id` FROM (SELECT `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`parquet_t0` TABLESAMPLE(100.0 PERCENT)) AS gen_subquery_0) AS parquet_t0
