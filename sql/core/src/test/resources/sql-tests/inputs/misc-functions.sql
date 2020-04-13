-- test for misc functions

-- typeof
select typeof(null);
select typeof(true);
select typeof(1Y), typeof(1S), typeof(1), typeof(1L);
select typeof(cast(1.0 as float)), typeof(1.0D), typeof(1.2);
select typeof(date '1986-05-23'),  typeof(timestamp '1986-05-23'), typeof(interval '23 days');
select typeof(x'ABCD'), typeof('SPARK');
select typeof(array(1, 2)), typeof(map(1, 2)), typeof(named_struct('a', 1, 'b', 'spark'));

-- try
select try(null) as try, null as notry;
select interval 1 day / 0 as notry;
select try(interval 1 day / 0) as try;
select cast('1-1' as double) as notry;
select try(cast('1-1' as double)) as try;
select cast(128 as tinyint) as notry;
select try(cast(128 as tinyint)) as try;
select encode('abc', 'utf-99') as notry;
select try(encode('abc', 'utf-99')) as try;
select date_format(timestamp '2019-10-06', 'yyyy-MM-dd uucc') as notry;
select try(date_format(timestamp '2019-10-06', 'yyyy-MM-dd uucc')) as try;
select regexp_extract('1a 2b 14m', '\\d+', 1) as notry;
select try(regexp_extract('1a 2b 14m', '\\d+', 1)) as try;
select format_string('Hello %s %s', 'World') as notry;
select try(format_string('Hello %s %s', 'World')) as try;
