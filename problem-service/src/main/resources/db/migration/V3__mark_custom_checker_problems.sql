UPDATE problem.problem_versions version
SET checker_type = 'CUSTOM',
    custom_checker_object_key = 's3://olimp-tests/checkers/cf-2010-c2.py'
FROM problem.problems problem
WHERE version.problem_id = problem.problem_id
  AND problem.slug = 'cf-2010-c2'
  AND version.active = TRUE;

UPDATE problem.problem_versions version
SET checker_type = 'CUSTOM',
    custom_checker_object_key = 's3://olimp-tests/checkers/cf-2039-h1.py'
FROM problem.problems problem
WHERE version.problem_id = problem.problem_id
  AND problem.slug = 'cf-2039-h1'
  AND version.active = TRUE;

UPDATE problem.problem_versions version
SET checker_type = 'CUSTOM',
    custom_checker_object_key = 's3://olimp-tests/checkers/cf-2039-h2.py'
FROM problem.problems problem
WHERE version.problem_id = problem.problem_id
  AND problem.slug = 'cf-2039-h2'
  AND version.active = TRUE;

UPDATE problem.problem_versions version
SET checker_type = 'CUSTOM',
    custom_checker_object_key = 's3://olimp-tests/checkers/cf-2065-e.py'
FROM problem.problems problem
WHERE version.problem_id = problem.problem_id
  AND problem.slug = 'cf-2065-e'
  AND version.active = TRUE;
