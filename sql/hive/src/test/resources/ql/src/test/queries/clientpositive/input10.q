CREATE TABLE TEST10(key INT, value STRING) PARTITIONED BY(ds STRING, hr STRING) STORED AS TEXTFILE;

EXPLAIN
DESCRIBE TEST10;

DESCRIBE TEST10;



