SELECT x.key FROM parquet_t1 x JOIN parquet_t1 y ON x.key = y.key
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `key` FROM (SELECT `gen_attr` FROM (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_0 INNER JOIN (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_1 ON (`gen_attr` = `gen_attr`)) AS x
