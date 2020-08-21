--
-- Context Store Table
--
CREATE TABLE alpakkeer.alpakkeer__context_store (
    job VARCHAR(128) PRIMARY KEY,
    inserted TIMESTAMP NOT NULL,
    type VARCHAR(256) NOT NULL,
    value JSONB NOT NULL
);