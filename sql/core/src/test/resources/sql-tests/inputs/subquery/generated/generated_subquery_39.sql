-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b, (SELECT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b, (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b, (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b, (SELECT no_match_table.a FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b, (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b, (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table LEFT OUTER JOIN join_table ON no_match_table.a = join_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST