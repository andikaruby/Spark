-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b, (SELECT no_match_table.a FROM no_match_table ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b, (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b, (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=SELECT, subquery_type=SCALAR, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b, (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table) AS subqueryAlias FROM outer_table ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST, subqueryAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT subqueryAlias.a FROM (SELECT DISTINCT no_match_table.a FROM no_match_table ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias ORDER BY a DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT subqueryAlias.a FROM (SELECT no_match_table.a FROM no_match_table ORDER BY a DESC NULLS FIRST LIMIT 1) AS subqueryAlias ORDER BY a DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=IN, is_correlated=false, distinct=true, subquery_operator=LIMIT 10
SELECT subqueryAlias.a FROM (SELECT DISTINCT no_match_table.a FROM no_match_table ORDER BY a DESC NULLS FIRST LIMIT 10) AS subqueryAlias ORDER BY a DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=IN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table GROUP BY no_match_table.b) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=IN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table GROUP BY no_match_table.b) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=IN, is_correlated=false, distinct=false, subquery_operator=LIMIT 10
SELECT subqueryAlias.a FROM (SELECT no_match_table.a FROM no_match_table ORDER BY a DESC NULLS FIRST LIMIT 10) AS subqueryAlias ORDER BY a DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table GROUP BY no_match_table.b) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=no_match_table, outer_table=outer_table, subqueryClause=FROM, subquery_type=IN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT subqueryAlias.aggFunctionAlias FROM (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table GROUP BY no_match_table.b) AS subqueryAlias ORDER BY aggFunctionAlias DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=true, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT DISTINCT no_match_table.a FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=true, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT no_match_table.a FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT DISTINCT no_match_table.a FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT no_match_table.a FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_LESS_THAN, is_correlated=false, distinct=false, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b < (SELECT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_EQUALS, is_correlated=true, distinct=true, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b = (SELECT DISTINCT no_match_table.a FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_EQUALS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b = (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_EQUALS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b = (SELECT DISTINCT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_EQUALS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b = (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_EQUALS, is_correlated=true, distinct=true, subquery_operator=AGGREGATE(resultExpr=[COUNT(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b = (SELECT DISTINCT COUNT(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_EQUALS, is_correlated=true, distinct=false, subquery_operator=LIMIT 1
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b = (SELECT no_match_table.a FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a ORDER BY a DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_EQUALS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[no_match_table.b])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b = (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a GROUP BY no_match_table.b ORDER BY aggFunctionAlias DESC NULLS FIRST LIMIT 1) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST;
-- inner_table=no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a, outer_table=outer_table, subqueryClause=WHERE, subquery_type=SCALAR_PREDICATE_EQUALS, is_correlated=true, distinct=false, subquery_operator=AGGREGATE(resultExpr=[SUM(no_match_table.a) AS aggFunctionAlias],groupingExpr=[])
SELECT outer_table.a, outer_table.b FROM outer_table WHERE outer_table.b = (SELECT SUM(no_match_table.a) AS aggFunctionAlias FROM no_match_table INNER JOIN join_table ON no_match_table.a = join_table.a WHERE no_match_table.a = outer_table.a) ORDER BY a DESC NULLS FIRST, b DESC NULLS FIRST