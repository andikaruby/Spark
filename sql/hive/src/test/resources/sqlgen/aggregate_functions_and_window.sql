SELECT MAX(c) + COUNT(a) OVER () FROM parquet_t2 GROUP BY a, b
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `(max(c) + count(a) OVER (  ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING))` FROM (SELECT (`gen_attr` + `gen_attr`) AS `gen_attr` FROM (SELECT gen_subquery_1.`gen_attr`, gen_subquery_1.`gen_attr`, count(`gen_attr`) OVER (  ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS `gen_attr` FROM (SELECT max(`gen_attr`) AS `gen_attr`, `gen_attr` FROM (SELECT `a` AS `gen_attr`, `b` AS `gen_attr`, `c` AS `gen_attr`, `d` AS `gen_attr` FROM `default`.`parquet_t2`) AS gen_subquery_0 GROUP BY `gen_attr`, `gen_attr`) AS gen_subquery_1) AS gen_subquery_2) AS gen_subquery_3
