SELECT EXPLODE(arr) AS val, id
FROM parquet_t3
WHERE id > 2
ORDER BY val, id
LIMIT 5
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `val`, `gen_attr` AS `id` FROM (SELECT `gen_attr`, `gen_attr` FROM (SELECT gen_subquery_0.`gen_attr`, gen_subquery_0.`gen_attr`, gen_subquery_0.`gen_attr`, gen_subquery_0.`gen_attr` FROM (SELECT `arr` AS `gen_attr`, `arr2` AS `gen_attr`, `json` AS `gen_attr`, `id` AS `gen_attr` FROM `default`.`parquet_t3`) AS gen_subquery_0 WHERE (`gen_attr` > CAST(2 AS BIGINT))) AS gen_subquery_1 LATERAL VIEW explode(`gen_attr`) gen_subquery_2 AS `gen_attr` ORDER BY `gen_attr` ASC, `gen_attr` ASC LIMIT 5) AS parquet_t3