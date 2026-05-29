WITH custom_checkers(slug, object_key) AS (
    VALUES
    ('cf-1998-a', 's3://olimp-tests/1998/A/checker.py'),
    ('cf-1998-b', 's3://olimp-tests/1998/B/checker.py'),
    ('cf-2010-c2', 's3://olimp-tests/2010/C2/checker.py'),
    ('cf-2020-c', 's3://olimp-tests/2020/C/checker.py'),
    ('cf-2023-a', 's3://olimp-tests/2023/A/checker.py'),
    ('cf-2023-d', 's3://olimp-tests/2023/D/checker.py'),
    ('cf-2025-f', 's3://olimp-tests/2025/F/checker.py'),
    ('cf-2029-d', 's3://olimp-tests/2029/D/checker.py'),
    ('cf-2030-b', 's3://olimp-tests/2030/B/checker.py'),
    ('cf-2031-c', 's3://olimp-tests/2031/C/checker.py'),
    ('cf-2032-b', 's3://olimp-tests/2032/B/checker.py'),
    ('cf-2032-e', 's3://olimp-tests/2032/E/checker.py'),
    ('cf-2034-d', 's3://olimp-tests/2034/D/checker.py'),
    ('cf-2034-g2', 's3://olimp-tests/2034/G2/checker.py'),
    ('cf-2034-h', 's3://olimp-tests/2034/H/checker.py'),
    ('cf-2035-c', 's3://olimp-tests/2035/C/checker.py'),
    ('cf-2037-c', 's3://olimp-tests/2037/C/checker.py'),
    ('cf-2038-c', 's3://olimp-tests/2038/C/checker.py'),
    ('cf-2039-h1', 's3://olimp-tests/2039/H1/checker.py'),
    ('cf-2039-h2', 's3://olimp-tests/2039/H2/checker.py'),
    ('cf-2041-l', 's3://olimp-tests/2041/L/checker.py'),
    ('cf-2042-e', 's3://olimp-tests/2042/E/checker.py'),
    ('cf-2045-c', 's3://olimp-tests/2045/C/checker.py'),
    ('cf-2045-h', 's3://olimp-tests/2045/H/checker.py'),
    ('cf-2045-m', 's3://olimp-tests/2045/M/checker.py'),
    ('cf-2046-c', 's3://olimp-tests/2046/C/checker.py'),
    ('cf-2046-e1', 's3://olimp-tests/2046/E1/checker.py'),
    ('cf-2046-e2', 's3://olimp-tests/2046/E2/checker.py'),
    ('cf-2046-f2', 's3://olimp-tests/2046/F2/checker.py'),
    ('cf-2048-c', 's3://olimp-tests/2048/C/checker.py'),
    ('cf-2048-e', 's3://olimp-tests/2048/E/checker.py'),
    ('cf-2049-c', 's3://olimp-tests/2049/C/checker.py'),
    ('cf-2057-c', 's3://olimp-tests/2057/C/checker.py'),
    ('cf-2057-g', 's3://olimp-tests/2057/G/checker.py'),
    ('cf-2061-b', 's3://olimp-tests/2061/B/checker.py'),
    ('cf-2062-e1', 's3://olimp-tests/2062/E1/checker.py'),
    ('cf-2062-g', 's3://olimp-tests/2062/G/checker.py'),
    ('cf-2065-e', 's3://olimp-tests/2065/E/checker.py'),
    ('cf-2066-f', 's3://olimp-tests/2066/F/checker.py')
)
UPDATE problem.problem_versions version
SET checker_type = 'CUSTOM',
    custom_checker_object_key = custom_checkers.object_key
FROM problem.problems problem
JOIN custom_checkers ON custom_checkers.slug = problem.slug
WHERE version.problem_id = problem.problem_id
  AND version.active = TRUE;
