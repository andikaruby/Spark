-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE EXISTS (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=LIMIT 10
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 10) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=NOT_EXISTS, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE NOT EXISTS (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a GROUP BY null_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a GROUP BY null_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b, (SELECT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b, (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a GROUP BY null_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b, (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a GROUP BY null_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a WHERE null_table.a = outer_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b, (SELECT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b, (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT outer_table.a, outer_table.b, (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT subqueryAlias.a FROM (SELECT DISTINCT null_table.a FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias ORDER BY a DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(null_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT DISTINCT SUM(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(null_table.a) AS aggFunctionAlias],groupingExpr=[null_table.b])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT DISTINCT COUNT(null_table.a) AS aggFunctionAlias FROM null_table LEFT OUTER JOIN join_table ON null_table.a = join_table.a GROUP BY null_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST