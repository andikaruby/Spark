DROP TABLE IF EXISTS DECIMAL_TEXT;
DROP TABLE IF EXISTS DECIMAL_RC;
DROP TABLE IF EXISTS DECIMAL_LAZY_COL;
DROP TABLE IF EXISTS DECIMAL_SEQUENCE;

CREATE TABLE DECIMAL_TEXT (key decimal, value int) 
ROW FORMAT DELIMITED
   FIELDS TERMINATED BY ' '
STORED AS TEXTFILE;

LOAD DATA LOCAL INPATH '../data/files/kv7.txt' INTO TABLE DECIMAL_TEXT;

SELECT * FROM DECIMAL_TEXT ORDER BY key, value;

CREATE TABLE DECIMAL_RC
STORED AS RCFile AS
SELECT * FROM DECIMAL_TEXT;

CREATE TABLE DECIMAL_LAZY_COL
ROW FORMAT SERDE "org.apache.hadoop.hive.serde2.columnar.ColumnarSerDe"
STORED AS RCFile AS
SELECT * FROM DECIMAL_RC;

CREATE TABLE DECIMAL_SEQUENCE
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\001'
COLLECTION ITEMS TERMINATED BY '\002'
MAP KEYS TERMINATED BY '\003'
STORED AS SEQUENCEFILE AS
SELECT * FROM DECIMAL_LAZY_COL ORDER BY key;

SELECT * FROM DECIMAL_SEQUENCE ORDER BY key, value;

DROP TABLE IF EXISTS DECIMAL_TEXT;
DROP TABLE IF EXISTS DECIMAL_RC;
DROP TABLE IF EXISTS DECIMAL_LAZY_COL;
DROP TABLE IF EXISTS DECIMAL_SEQUENCE;
