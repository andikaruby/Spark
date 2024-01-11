CREATE TEMPORARY VIEW inner_table(a, b) AS VALUES
    (1, 1),
    (2, 2),
    (3, 3),
    (4, 4),
    (5, 5),
    (8, 8),
    (9, 9);

CREATE TEMPORARY VIEW outer_table(a, b) AS VALUES
    (1, 1),
    (2, 1),
    (3, 3),
    (6, 6),
    (7, 7),
    (9, 9);

CREATE TEMPORARY VIEW no_match_table(a, b) AS VALUES
    (1000, 1000);

CREATE TEMPORARY VIEW join_table(a, b) AS VALUES
    (1, 1),
    (2, 1),
    (3, 3),
    (7, 8),
    (5, 6);

CREATE TEMPORARY VIEW null_table(a, b) AS SELECT CAST(null AS int), CAST(null as int);

-- innerTable=inner_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT no_match_table.a FROM no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT inner_table.a FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table INNER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT null_table.a FROM null_table INNER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT null_table.a FROM null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT no_match_table.a FROM no_match_table ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT inner_table.a FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table INNER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table INNER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT null_table.a FROM null_table ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT inner_table.a FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table INNER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table INNER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table INNER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table INNER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT inner_table.a FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT null_table.a FROM null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT null_table.a FROM null_table ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT inner_table.a FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table INNER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table INNER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table INNER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table INNER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT no_match_table.a FROM no_match_table ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT inner_table.a FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT inner_table.a FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT no_match_table.a FROM no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT no_match_table.a FROM no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT null_table.a FROM null_table ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table INNER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT null_table.a FROM null_table INNER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT no_match_table.a FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT no_match_table.a FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT inner_table.a FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT no_match_table.a FROM no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT no_match_table.a FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT inner_table.a FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table INNER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table INNER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table INNER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table INNER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT inner_table.a FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT no_match_table.a FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT no_match_table.a FROM no_match_table ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table INNER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table INNER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT inner_table.a FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT null_table.a FROM null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT null_table.a FROM null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT null_table.a, null_table.b FROM null_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table INNER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT null_table.a FROM null_table INNER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT inner_table.a FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table RIGHT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table INNER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT null_table.a FROM null_table INNER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table INTERSECT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=no_match_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT no_match_table.a, no_match_table.b FROM no_match_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table RIGHT OUTER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT null_table.a, null_table.b FROM null_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT null_table.a, null_table.b FROM null_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=null_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=false, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT null_table.a FROM null_table ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=no_match_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT no_match_table.a FROM no_match_table ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table EXCEPT SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=inner_table, outerTable=outer_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;

-- innerTable=(SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias, outerTable=null_table, subqueryLocation=WHERE, subqueryType=EXISTS, isCorrelated=false, subqueryDistinct=true, subqueryOperator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT inner_table.a, inner_table.b FROM inner_table UNION SELECT join_table.a, join_table.b FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST
