SELECT s.id FROM t0 TABLESAMPLE(100 PERCENT) s
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `id` FROM (SELECT `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`t0` TABLESAMPLE(100.0 PERCENT)) AS gen_subquery_0) AS s
