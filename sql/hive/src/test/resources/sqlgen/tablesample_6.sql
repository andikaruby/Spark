SELECT * FROM parquet_t0 TABLESAMPLE(0.1 PERCENT) WHERE 1=0
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `id` FROM (SELECT `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`parquet_t0` TABLESAMPLE(0.1 PERCENT)) AS gen_subquery_0 WHERE (1 = 0)) AS parquet_t0
