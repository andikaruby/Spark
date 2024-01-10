-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=false, subquery_operator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=false, subquery_operator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=true, subquery_operator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT DISTINCT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT DISTINCT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT DISTINCT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT DISTINCT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=LIMIT 10
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b FROM null_table WHERE NOT EXISTS (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=LIMIT 1
SELECT null_table.a, null_table.b, (SELECT DISTINCT inner_table.a FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM null_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b, (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a GROUP BY inner_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM null_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT null_table.a, null_table.b, (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a) AS subqueryAlias FROM null_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=inner_table INNER JOIN join_table ON inner_table.a = join_table.a, outer_table=null_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT null_table.a, null_table.b, (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table INNER JOIN join_table ON inner_table.a = join_table.a WHERE inner_table.a = null_table.a GROUP BY inner_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM null_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST