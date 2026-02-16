-- Add version column for optimistic locking
ALTER TABLE books ADD COLUMN version INT DEFAULT 0 NOT NULL;
