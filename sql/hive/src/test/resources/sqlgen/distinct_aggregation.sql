SELECT COUNT(DISTINCT id) FROM parquet_t0
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `count(DISTINCT id)` FROM (SELECT count(DISTINCT `gen_attr`) AS `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`parquet_t0`) AS gen_subquery_0) AS gen_subquery_1