-- Initialization script for PostgreSQL
-- Create extensions if needed

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create initial admin user (password: admin123)
-- This will be created by the application on first run
