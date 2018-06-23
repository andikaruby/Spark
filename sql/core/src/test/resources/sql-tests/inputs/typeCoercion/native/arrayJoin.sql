SELECT array_join(array(true, false), ', ');
SELECT array_join(array(2Y, 1Y), ', ');
SELECT array_join(array(2S, 1S), ', ');
SELECT array_join(array(2, 1), ', ');
SELECT array_join(array(2L, 1L), ', ');
SELECT array_join(array(9223372036854775809, 9223372036854775808), ', ');
SELECT array_join(array(2.0D, 1.0D), ', ');
SELECT array_join(array(float(2.0), float(1.0)), ', ');
SELECT array_join(array(date '2016-03-14', date '2016-03-13'), ', ');
SELECT array_join(array(timestamp '2016-11-15 20:54:00.000', timestamp '2016-11-12 20:54:00.000'), ', ');
SELECT array_join(array('a', 'b'), ', ');
SELECT array_join(array(array('a', 'b'), array('c', 'd')), ', ');
SELECT array_join(array(struct('a', 1), struct('b', 2)), ', ');
SELECT array_join(array(map('a', 1), map('b', 2)), ', ');