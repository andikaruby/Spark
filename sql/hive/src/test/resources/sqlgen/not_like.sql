SELECT id FROM t0 WHERE id + 5 NOT LIKE '1%'
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `id` FROM (SELECT `gen_attr` FROM (SELECT `id` AS `gen_attr` FROM `default`.`t0`) AS gen_subquery_0 WHERE (NOT CAST((`gen_attr` + CAST(5 AS BIGINT)) AS STRING) LIKE "1%")) AS t0
