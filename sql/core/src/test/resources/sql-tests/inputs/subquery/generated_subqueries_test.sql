CREATE TEMPORARY VIEW inner_table (a, b) AS VALUES
    (1, 10),
    (2, 20),
    (3, 30),
    (4, 40),
    (5, 50),
    (8, 80),
    (9, 90);
CREATE TEMPORARY VIEW outer_table (a, b) AS VALUES
    (1, 100),
    (2, 200),
    (3, 300),
    (4, 400),
    (6, 600),
    (7, 700),
    (10, 1000);
CREATE TEMPORARY VIEW no_match_inner_table (a, b) AS VALUES
    (5, 50),
    (8, 80),
    (9, 90);
CREATE TEMPORARY VIEW no_match_outer_table (a, b) AS VALUES
    (6, 600),
    (7, 700),
    (10, 1000);
CREATE TEMPORARY VIEW join_table (a, b) AS VALUES
    (1, 10),
    (3, 30),
    (2, 200),
    (3, 300),
    (4, 40),
    (4, 400),
    (5, 50),
    (6, 600);

-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT outer_table.b, (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT outer_table.b, (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT outer_table.b, (SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT outer_table.b, (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT outer_table.b, (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT outer_table.b, (SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT outer_table.b, (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT outer_table.b, (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT outer_table.b, (SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT outer_table.b, (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT outer_table.b, (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT outer_table.b, (SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT outer_table.b, (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT outer_table.b, (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT outer_table.b, (SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT outer_table.b, (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT outer_table.b, (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT outer_table.b, (SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT outer_table.b, (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT outer_table.b, (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT outer_table.b, (SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT outer_table.b, (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT outer_table.b, (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT outer_table.b, (SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM outer_table ORDER BY outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT subquery_column_alias FROM (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT subquery_column_alias FROM (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT subquery_column_alias FROM (SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT subquery_column_alias FROM (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=FROM,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT subquery_column_alias FROM (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias ORDER BY subquery_column_alias NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE EXISTS(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM outer_table WHERE outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT no_match_outer_table.b, (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT no_match_outer_table.b, (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT no_match_outer_table.b, (SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT no_match_outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT no_match_outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT no_match_outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT no_match_outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT no_match_outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT no_match_outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT no_match_outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT no_match_outer_table.b, (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT no_match_outer_table.b, (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT no_match_outer_table.b, (SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT no_match_outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT no_match_outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT no_match_outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT no_match_outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT no_match_outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT no_match_outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT no_match_outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT no_match_outer_table.b, (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT no_match_outer_table.b, (SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT no_match_outer_table.b, (SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT no_match_outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT no_match_outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT no_match_outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT no_match_outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT no_match_outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT no_match_outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT no_match_outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT no_match_outer_table.b, (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT no_match_outer_table.b, (SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT no_match_outer_table.b, (SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT no_match_outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT no_match_outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT no_match_outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT no_match_outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT no_match_outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT no_match_outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT no_match_outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b WHERE inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT SUM(inner_table.a) AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT SUM(inner_table.a) AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT COUNT(inner_table.a) AS subquery_column_alias FROM inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table INNER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table LEFT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT inner_table.a AS subquery_column_alias FROM inner_table RIGHT OUTER JOIN join_table ON inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT no_match_outer_table.b, (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT no_match_outer_table.b, (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT no_match_outer_table.b, (SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT no_match_outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT no_match_outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT no_match_outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT no_match_outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT no_match_outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT no_match_outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT no_match_outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT no_match_outer_table.b, (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT no_match_outer_table.b, (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT no_match_outer_table.b, (SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT no_match_outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT no_match_outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT no_match_outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT no_match_outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT no_match_outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT no_match_outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT no_match_outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT no_match_outer_table.b, (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT no_match_outer_table.b, (SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT no_match_outer_table.b, (SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT no_match_outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT no_match_outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT no_match_outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT no_match_outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT no_match_outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT no_match_outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT no_match_outer_table.b, (SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT no_match_outer_table.b, (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT no_match_outer_table.b, (SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT no_match_outer_table.b, (SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT no_match_outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT no_match_outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT no_match_outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT no_match_outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT no_match_outer_table.b, (SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT no_match_outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT no_match_outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=SELECT,subquery_type=NA,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT no_match_outer_table.b, (SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) AS subquery_alias FROM no_match_outer_table ORDER BY no_match_outer_table.b, subquery_alias NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT IN,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a NOT IN(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a   ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a  ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 10) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type=NOT EXISTS,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE NOT EXISTS(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias    ) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b WHERE no_match_inner_table.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=True,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias WHERE subquery_set_operation_alias.a = no_match_outer_table.a  ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=True,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT DISTINCT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('SUM', False)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT SUM(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=AGGREGATE,subquery_operator_type=('COUNT', True)
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT COUNT(no_match_inner_table.a) AS subquery_column_alias FROM no_match_inner_table  GROUP BY a ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=1
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=LIMIT,subquery_operator_type=10
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=ORDER BY,subquery_operator_type=None
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=INNER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table INNER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=LEFT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table LEFT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=JOIN,subquery_operator_type=RIGHT OUTER JOIN
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT no_match_inner_table.a AS subquery_column_alias FROM no_match_inner_table RIGHT OUTER JOIN join_table ON no_match_inner_table.b = join_table.b   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=INTERSECT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table INTERSECT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=UNION
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table UNION SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
-- subquery_in=WHERE,subquery_type==,is_correlated=False,distinct=False,subquery_operator=SET_OP,subquery_operator_type=EXCEPT
SELECT a, b FROM no_match_outer_table WHERE no_match_outer_table.a =(SELECT subquery_set_operation_alias.a AS subquery_column_alias FROM (SELECT a, b FROM no_match_inner_table EXCEPT SELECT a, b FROM join_table) AS subquery_set_operation_alias   ORDER BY subquery_column_alias DESC NULLS FIRST LIMIT 1) ORDER BY a, b NULLS FIRST;
