-- Retire chat access without deleting historical messages or changing earlier migrations.
DELETE ur FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE r.name='PERM_CHAT_ACCESS';
DELETE FROM roles WHERE name='PERM_CHAT_ACCESS';
