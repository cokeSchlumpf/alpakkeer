--
-- SQL query to select a context value by its name
--
SELECT type, value FROM {{ schema }}.{{ table }} WHERE job = '{{ job }}'