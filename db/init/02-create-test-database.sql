CREATE DATABASE test OWNER icares;
\connect test
\i /docker-entrypoint-initdb.d/00-schema.sql
\i /docker-entrypoint-initdb.d/01-seed.sql
\connect dev
