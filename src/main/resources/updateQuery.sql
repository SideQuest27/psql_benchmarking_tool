\set aid random(1, 40000000)
UPDATE pgbench_accounts SET abalance = abalance + 100 WHERE aid = :aid;

\set aid random(1, 40000000)
UPDATE pgbench_accounts SET abalance = abalance + 100 WHERE aid = :aid;

\set aid random(1, 40000000)
UPDATE pgbench_accounts SET abalance = abalance + 100 WHERE aid = :aid;