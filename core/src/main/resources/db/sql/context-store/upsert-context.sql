--
-- SQL query to insert or update entry in the context table
--
INSERT INTO {{ schema }}.{{ table }} (job, inserted, type, value)
VALUES ('{{ job }}', '{{ inserted }}', '{{ type }}', '{{ value }}')
ON CONFLICT (job)
DO UPDATE SET inserted = EXCLUDED.inserted, type = EXCLUDED.type, value = EXCLUDED.value