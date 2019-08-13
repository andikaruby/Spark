CREATE TEMPORARY VIEW t AS select '2011-05-06 07:08:09.1234567' as c;

select extract(year from c) from t;

select extract(isoyear from c) from t;

select extract(quarter from c) from t;

select extract(month from c) from t;

select extract(week from c) from t;

select extract(day from c) from t;

select extract(dayofweek from c) from t;

select extract(dow from c) from t;

select extract(isodow from c) from t;

select extract(doy from c) from t;

select extract(hour from c) from t;

select extract(minute from c) from t;

select extract(second from c) from t;

select extract(milliseconds from c) from t;
select extract(msec from c) from t;
select extract(msecs from c) from t;
select extract(millisecon from c) from t;
select extract(mseconds from c) from t;
select extract(ms from c) from t;

select extract(microseconds from c) from t;
select extract(usec from c) from t;
select extract(usecs from c) from t;
select extract(useconds from c) from t;
select extract(microsecon from c) from t;
select extract(us from c) from t;

select extract(epoch from c) from t;

select extract(not_supported from c) from t;
