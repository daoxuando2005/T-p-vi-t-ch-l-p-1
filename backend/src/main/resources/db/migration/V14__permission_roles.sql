-- Create available permissions once, without changing user passwords, enabled
-- flags, granted permissions, family links, or re-creating deleted demo users.
INSERT IGNORE INTO roles(name) VALUES
('PERM_ASSIGN_HOMEWORK'),('PERM_VIEW_ALPHABET'),('PERM_MANAGE_CLASSES'),
('PERM_PARENT_TRACKING'),('PERM_EDIT_ALPHABET'),('PERM_ADD_ALPHABET');
