ALTER TABLE user_account ALTER COLUMN role SET DEFAULT 'VIEWER';
UPDATE user_account SET role = 'VIEWER' WHERE role = 'OWNER';
