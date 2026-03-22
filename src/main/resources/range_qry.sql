
--EXPLAIN (ANALYZE, COSTS OFF) use this to see the planning and executing metrics
SELECT abalance FROM pgbench_accounts WHERE aid BETWEEN 100 AND 5000;