SELECT a, b, sum(c) FROM parquet_t2 GROUP BY a, b GROUPING SETS (a) ORDER BY a, b
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `a`, `gen_attr` AS `b`, `gen_attr` AS `sum(c)` FROM (SELECT `gen_attr` AS `gen_attr`, `gen_attr` AS `gen_attr`, sum(`gen_attr`) AS `gen_attr` FROM (SELECT `a` AS `gen_attr`, `b` AS `gen_attr`, `c` AS `gen_attr`, `d` AS `gen_attr` FROM `default`.`parquet_t2`) AS gen_subquery_0 GROUP BY `gen_attr`, `gen_attr` GROUPING SETS((`gen_attr`)) ORDER BY `gen_attr` ASC, `gen_attr` ASC) AS gen_subquery_1
