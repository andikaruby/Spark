SELECT `gen_attr` AS `CASE WHEN ((id % CAST(2 AS BIGINT)) > CAST(0 AS BIGINT)) THEN 0 WHEN ((id % CAST(2 AS BIGINT)) = CAST(0 AS BIGINT)) THEN 1 END` FROM (SELECT CASE WHEN ((`gen_attr` % CAST(2 AS BIGINT)) > CAST(0 AS BIGINT)) THEN 0 WHEN ((`gen_attr` % CAST(2 AS BIGINT)) = CAST(0 AS BIGINT)) THEN 1 END AS `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`parquet_t0`) AS gen_subquery_0) AS gen_subquery_1