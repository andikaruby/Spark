SELECT TRANSFORM (key) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
USING 'cat' AS (tKey) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
FROM parquet_t1
--------------------------------------------------------------------------------
SELECT `gen_attr` AS `tKey` FROM (SELECT TRANSFORM (`gen_attr`) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t' USING 'cat' AS (`gen_attr` string) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t' FROM (SELECT `key` AS `gen_attr`, `value` AS `gen_attr` FROM `default`.`parquet_t1`) AS gen_subquery_0) AS gen_subquery_1
