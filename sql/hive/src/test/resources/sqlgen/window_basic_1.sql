SELECT MAX(value) OVER (PARTITION BY key % 3) FROM parquet_t1
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `max(value) OVER (PARTITION BY (key % CAST(3 AS BIGINT))  ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)` FROM (SELECT `gen_attr` FROM (SELECT gen_subquery_1.`gen_attr`, gen_subquery_1.`gen_attr`, max(`gen_attr`) OVER (PARTITION BY `gen_attr`  ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS `gen_attr` FROM (SELECT `gen_attr`, (`gen_attr` % CAST(3 AS BIGINT)) AS `gen_attr` FROM (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_0) AS gen_subquery_1) AS gen_subquery_2) AS gen_subquery_3