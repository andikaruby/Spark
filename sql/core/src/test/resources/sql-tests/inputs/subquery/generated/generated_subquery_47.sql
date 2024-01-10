-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=true, distinct=false, subquery_operator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT inner_table.a FROM inner_table WHERE inner_table.a = no_match_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=true, distinct=false, subquery_operator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT inner_table.a FROM inner_table WHERE inner_table.a = no_match_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT DISTINCT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=false, distinct=true, subquery_operator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT DISTINCT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=false, distinct=false, subquery_operator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b IN (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=true, subquery_operator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT DISTINCT inner_table.a FROM inner_table WHERE inner_table.a = no_match_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=true, subquery_operator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT DISTINCT inner_table.a FROM inner_table WHERE inner_table.a = no_match_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=false, subquery_operator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT inner_table.a FROM inner_table WHERE inner_table.a = no_match_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=false, subquery_operator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT inner_table.a FROM inner_table WHERE inner_table.a = no_match_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT DISTINCT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=true, subquery_operator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT DISTINCT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=false, subquery_operator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT inner_table.a FROM inner_table ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE no_match_table.b NOT IN (SELECT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table WHERE inner_table.a = no_match_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT inner_table.a FROM inner_table WHERE inner_table.a = no_match_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT SUM(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[inner_table.b])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a GROUP BY inner_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(inner_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT DISTINCT COUNT(inner_table.a) AS aggFunctionAlias FROM inner_table WHERE inner_table.a = no_match_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=false, subquery_operator=LIMIT 1
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT inner_table.a FROM inner_table WHERE inner_table.a = no_match_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=inner_table, outer_table=no_match_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=false, subquery_operator=LIMIT 10
SELECT no_match_table.a, no_match_table.b FROM no_match_table WHERE EXISTS (SELECT inner_table.a FROM inner_table WHERE inner_table.a = no_match_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST