-- A3.1 retire legacy employee management permissions after consumer migration.

DELETE role_menu
FROM `t_role_menu` role_menu
JOIN `t_menu` legacy_menu ON legacy_menu.`menu_id` = role_menu.`menu_id`
WHERE legacy_menu.`api_perms` LIKE 'system:employee:%'
   OR legacy_menu.`web_perms` LIKE 'system:employee:%';

DELETE FROM `t_menu`
WHERE `api_perms` LIKE 'system:employee:%'
   OR `web_perms` LIKE 'system:employee:%';
