-- SELECT 'abc' SIMILAR TO 'abc' AS `true`;
SELECT 'abc' SIMILAR TO 'abc' AS `true`;
-- SELECT 'abc' SIMILAR TO 'a' AS `false`;
SELECT 'abc' SIMILAR TO 'a' AS `false`;
-- SELECT 'abc' SIMILAR TO '%(b|d)%' AS `true`;
SELECT 'abc' SIMILAR TO '%(b|d)%' AS `true`;
-- SELECT 'abc' SIMILAR TO '(b|c)%' AS `false`;
SELECT 'abc' SIMILAR TO '(b|c)%' AS `false`;
-- SELECT '|bd' SIMILAR TO '|(b|d)*' AS `false`;
SELECT '|bd' SIMILAR TO '|(b|d)*' AS `false`;
-- SELECT '|bd' SIMILAR TO '\|(b|d)*' AS `true`;
SELECT '|bd' SIMILAR TO '\|(b|d)*' AS `true`;
-- SELECT '|bd' SIMILAR TO '\|(b|d)*' ESCAPE '\' AS `true`;
SELECT '|bd' SIMILAR TO '\|(b|d)*' ESCAPE '\' AS `true`;
-- SELECT '|bd' SIMILAR TO '\|(b|d)*' ESCAPE '#' AS `false`;
SELECT '|bd' SIMILAR TO '\|(b|d)*' ESCAPE '#' AS `false`;
-- SELECT '|bd' SIMILAR TO '#|(b|d)*' ESCAPE '#' AS `true`;
SELECT '|bd' SIMILAR TO '#|(b|d)*' ESCAPE '#' AS `true`;
-- SELECT 'abd' SIMILAR TO 'a(b|d)*' AS `true`;
SELECT 'abd' SIMILAR TO 'a(b|d)*' AS `true`;
-- SELECT 'abd' SIMILAR TO 'a.*' AS `false`;
SELECT 'abd' SIMILAR TO 'a.*' AS `false`;
-- SELECT 'abd' SIMILAR TO 'a%' AS `true`;
SELECT 'abd' SIMILAR TO 'a%' AS `true`;
-- SELECT 'abd' SIMILAR TO '.*' AS `false`;
SELECT 'abd' SIMILAR TO '.*' AS `false`;
-- SELECT 'abd' SIMILAR TO '%' AS `true`;
SELECT 'abd' SIMILAR TO '%' AS `true`;
