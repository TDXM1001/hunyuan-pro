-- A3.2 P5：稳定访问控制接口已完成切换，正式退役旧角色与菜单权限节点。
-- 先移除角色授权关系，再删除旧权限节点；角色页、菜单页和 access.* 能力节点保持不变。

DELETE role_menu
FROM `t_role_menu` role_menu
JOIN `t_menu` legacy_menu ON legacy_menu.`menu_id` = role_menu.`menu_id`
WHERE legacy_menu.`api_perms` LIKE 'system:role:%'
   OR legacy_menu.`web_perms` LIKE 'system:role:%'
   OR legacy_menu.`api_perms` LIKE 'system:menu:%'
   OR legacy_menu.`web_perms` LIKE 'system:menu:%';

DELETE FROM `t_menu`
WHERE `api_perms` LIKE 'system:role:%'
   OR `web_perms` LIKE 'system:role:%'
   OR `api_perms` LIKE 'system:menu:%'
   OR `web_perms` LIKE 'system:menu:%';
