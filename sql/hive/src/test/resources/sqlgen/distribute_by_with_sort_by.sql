SELECT id FROM parquet_t0 DISTRIBUTE BY id SORT BY id
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `id` FROM (SELECT `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`parquet_t0`) AS gen_subquery_0 CLUSTER BY `gen_attr`) AS parquet_t0