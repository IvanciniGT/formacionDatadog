-- Runs on first start of the postgres container (docker-entrypoint-initdb.d) and again on every
-- Kubernetes deploy (Job shop-db-init), so everything here must be idempotent.

-- 1) Application schema and data ------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    category  VARCHAR(50)  NOT NULL,
    price     NUMERIC(10,2) NOT NULL,
    stock     INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id         BIGSERIAL PRIMARY KEY,
    customer   VARCHAR(100) NOT NULL,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity   INTEGER NOT NULL,
    total      NUMERIC(10,2) NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- No index on product_id on purpose: the N+1 report gets slower as orders grow,
-- and "add the index" is part of the DBM exercise.

INSERT INTO products (name, category, price, stock)
SELECT * FROM (VALUES
 ('Keyboard',        'peripherals', 49.90, 500),
 ('Mouse',           'peripherals', 19.90, 800),
 ('Monitor 27"',     'displays',   249.00, 120),
 ('USB-C hub',       'peripherals', 34.50, 300),
 ('Laptop stand',    'furniture',   59.00, 200),
 ('Desk lamp',       'furniture',   29.00, 250),
 ('Webcam',          'peripherals', 79.00, 150),
 ('Headset',         'audio',       99.00, 180),
 ('Speakers',        'audio',      129.00,  90),
 ('Ultrawide 34"',   'displays',   499.00,  40)) AS seed(name, category, price, stock)
WHERE NOT EXISTS (SELECT 1 FROM products);

-- 2) Datadog user for Database Monitoring (DBM) ----------------------------------
-- https://docs.datadoghq.com/database_monitoring/setup_postgres/selfhosted/
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'datadog') THEN
    CREATE ROLE datadog LOGIN PASSWORD 'datadog';
  END IF;
END $$;
ALTER ROLE datadog INHERIT;
GRANT pg_monitor TO datadog;
GRANT SELECT ON pg_stat_database TO datadog;

CREATE SCHEMA IF NOT EXISTS datadog;
GRANT USAGE ON SCHEMA datadog TO datadog;
GRANT USAGE ON SCHEMA public TO datadog;

-- explain plans: the agent calls datadog.explain_statement(<query>) as the datadog user
CREATE OR REPLACE FUNCTION datadog.explain_statement(
   l_query TEXT,
   OUT explain JSON
)
RETURNS SETOF JSON AS
$$
DECLARE
curs REFCURSOR;
plan JSON;

BEGIN
   OPEN curs FOR EXECUTE pg_catalog.concat('EXPLAIN (FORMAT JSON) ', l_query);
   FETCH curs INTO plan;
   CLOSE curs;
   RETURN QUERY SELECT plan;
END;
$$
LANGUAGE 'plpgsql'
RETURNS NULL ON NULL INPUT
SECURITY DEFINER;

-- pg_stat_statements is loaded by the -c shared_preload_libraries flag in docker-compose
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
