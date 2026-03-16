-- make sure to create a sequence on aid before running this script
-- CREATE SEQUENCE pgbench_accounts_aid_seq
--  START WITH 40000001;
INSERT INTO pgbench_accounts (aid, bid, abalance, filler)
VALUES (nextval('pgbench_accounts_aid_seq'), 1, 0, 'test_filler');