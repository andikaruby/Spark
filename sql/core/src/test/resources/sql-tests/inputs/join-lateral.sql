-- Test cases for lateral join

CREATE VIEW t1(c1, c2) AS VALUES (0, 1), (1, 2);
CREATE VIEW t2(c1, c2) AS VALUES (0, 2), (0, 3);

-- lateral join with single column select
SELECT * FROM t1, LATERAL (SELECT c1);
SELECT * FROM t1, LATERAL (SELECT c1 FROM t2);
SELECT * FROM t1, LATERAL (SELECT t1.c1 FROM t2);
SELECT * FROM t1, LATERAL (SELECT t1.c1 + t2.c1 FROM t2);

-- lateral join with star expansion
SELECT * FROM t1, LATERAL (SELECT *);
SELECT * FROM t1, LATERAL (SELECT * FROM t2);
SELECT * FROM t1, LATERAL (SELECT t1.*);
SELECT * FROM t1, LATERAL (SELECT t1.*, t2.* FROM t2);

-- lateral join with different join types
SELECT * FROM t1 JOIN LATERAL (SELECT c1 + c2 AS c3) ON c2 = c3;
SELECT * FROM t1 LEFT JOIN LATERAL (SELECT c1 + c2 AS c3) ON c2 = c3;
SELECT * FROM t1 CROSS JOIN LATERAL (SELECT c1 + c2 AS c3);
SELECT * FROM t1 NATURAL JOIN LATERAL (SELECT c1 + c2 AS c2);
SELECT * FROM t1 JOIN LATERAL (SELECT c1 + c2 AS c2) USING (c2);

-- lateral join without outer column references
SELECT * FROM LATERAL (SELECT * FROM t1);
SELECT * FROM t1, LATERAL (SELECT * FROM t2);
SELECT * FROM LATERAL (SELECT * FROM t1), LATERAL (SELECT * FROM t2);
SELECT * FROM LATERAL (SELECT * FROM t1) JOIN LATERAL (SELECT * FROM t2);

-- lateral join with subquery alias
SELECT a, b FROM t1, LATERAL (SELECT c1, c2) s(a, b);

-- lateral join with foldable outer query references
SELECT * FROM (SELECT 1 AS c1, 2 AS c2), LATERAL (SELECT c1, c2);

-- lateral join with correlated equality predicates
SELECT * FROM t1, LATERAL (SELECT c2 FROM t2 WHERE t1.c1 = t2.c1);

-- lateral join with correlated non-equality predicates
SELECT * FROM t1, LATERAL (SELECT c2 FROM t2 WHERE t1.c2 < t2.c2);

-- lateral join can only reference preceding FROM clause items
SELECT * FROM t1 JOIN LATERAL (SELECT t1.c1 AS a, t2.c1 AS b) s JOIN t2 ON s.b = t2.c1;

-- multiple lateral joins
SELECT * FROM t1,
LATERAL (SELECT c1 + c2 AS a),
LATERAL (SELECT c1 - c2 AS b),
LATERAL (SELECT a * b AS c);

-- lateral join in between regular joins
SELECT * FROM t1
LEFT OUTER JOIN LATERAL (SELECT c2 FROM t2 WHERE t1.c1 = t2.c1) s
LEFT OUTER JOIN t1 t3 ON s.c2 = t3.c2;

-- nested lateral join
SELECT * FROM t1, LATERAL (SELECT * FROM t2, LATERAL (SELECT c1));

-- uncorrelated scalar subquery inside lateral join
SELECT * FROM t1, LATERAL (SELECT c2, (SELECT MIN(c2) FROM t2));

-- correlated scalar subquery inside lateral join
SELECT * FROM t1, LATERAL (SELECT c1, (SELECT SUM(c2) FROM t2 WHERE c1 = t1.c1));

-- lateral join inside subquery
SELECT * FROM t1 WHERE c1 = (
  SELECT SUM(c2)
  FROM (SELECT c1 FROM t1) s,
  LATERAL (SELECT * FROM t2 WHERE s.c1 = t2.c1)
);

-- lateral join with COUNT aggregate
-- TODO: the expected result should be (1, 2, 0)
SELECT * FROM t1, LATERAL (SELECT COUNT(*) AS cnt FROM t2 WHERE c1 = t1.c1) WHERE cnt = 0;

-- lateral subquery with group by
SELECT * FROM t1 LEFT JOIN LATERAL (SELECT MIN(c2) FROM t2 WHERE c1 = t1.c1 GROUP BY c1);

-- lateral join inside CTE
WITH cte1 AS (
  SELECT c1 FROM t1
), cte2 AS (
  SELECT s.* FROM cte1, LATERAL (SELECT * FROM t2 WHERE c1 = cte1.c1) s
)
SELECT * FROM cte2;

-- clean up
DROP VIEW t1;
DROP VIEW t2;
