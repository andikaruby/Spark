SELECT id FROM orc_parquet_t0
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `id` FROM (SELECT `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`orc_parquet_t0`) AS gen_subquery_0) AS orc_parquet_t0