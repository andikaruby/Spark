SELECT CASE id WHEN 0 THEN 'foo' WHEN 1 THEN 'bar' END FROM parquet_t0
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `CASE WHEN (id = CAST(0 AS BIGINT)) THEN foo WHEN (id = CAST(1 AS BIGINT)) THEN bar END` FROM (SELECT CASE WHEN (`gen_attr` = CAST(0 AS BIGINT)) THEN "foo" WHEN (`gen_attr` = CAST(1 AS BIGINT)) THEN "bar" END AS `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`parquet_t0`) AS gen_subquery_0) AS gen_subquery_1
