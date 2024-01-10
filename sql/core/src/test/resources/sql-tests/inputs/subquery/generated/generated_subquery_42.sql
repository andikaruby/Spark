-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b IN (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b IN (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=true, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=false, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=true, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=false, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b NOT IN (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=false, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=true, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM no_match_table UNION SELECT * FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST