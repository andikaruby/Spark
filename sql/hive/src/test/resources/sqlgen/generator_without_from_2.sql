SELECT EXPLODE(ARRAY(1,2,3)) AS val
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `val` FROM (SELECT `gen_attr` FROM (SELECT 1) gen_subquery_1 LATERAL VIEW explode(array(1, 2, 3)) gen_subquery_2 AS `gen_attr`) AS gen_subquery_0