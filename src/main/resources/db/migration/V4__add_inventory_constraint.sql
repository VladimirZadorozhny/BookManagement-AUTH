-- Add check constraint to ensure available count never goes below zero
ALTER TABLE books ADD CONSTRAINT check_available_positive CHECK (available >= 0);
