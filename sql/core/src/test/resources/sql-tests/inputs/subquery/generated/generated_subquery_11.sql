-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT innerSubqueryAlias.a FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a GROUP BY innerSubqueryAlias.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a GROUP BY innerSubqueryAlias.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b, (SELECT innerSubqueryAlias.a FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b, (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a GROUP BY innerSubqueryAlias.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b, (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a GROUP BY innerSubqueryAlias.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias WHERE innerSubqueryAlias.a = outer_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b, (SELECT innerSubqueryAlias.a FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b, (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT outer_table.a, outer_table.b, (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT subqueryAlias.a FROM (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias ORDER BY a DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT DISTINCT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT subqueryAlias.a FROM (SELECT innerSubqueryAlias.a FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias ORDER BY a DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT COUNT(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=FROM, subquery_type=IN, is_correlated=false, distinct=true, subquery_operator=LIMIT 10
SELECT subqueryAlias.a FROM (SELECT DISTINCT innerSubqueryAlias.a FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias ORDER BY a DESC NULLS FIRST LIMIT 10) AS subqueryAlias ORDER BY a DESC NULLS FIRST;
-- inner_table=(SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias, outer_table=outer_table, subqueryClause=FROM, subquery_type=IN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(innerSubqueryAlias.a) AS aggFunctionAlias],groupingExpr=[innerSubqueryAlias.b])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT DISTINCT SUM(innerSubqueryAlias.a) AS aggFunctionAlias FROM (SELECT * FROM inner_table INTERSECT SELECT * FROM join_table) AS innerSubqueryAlias GROUP BY innerSubqueryAlias.b) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST