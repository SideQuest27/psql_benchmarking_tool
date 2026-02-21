-- contents of range_query.sql
SELECT abalance FROM pgbench_accounts WHERE aid BETWEEN 100 AND 5000;