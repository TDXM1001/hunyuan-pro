-- A3.3 P5：岗位稳定能力已完成切换，正式退役旧岗位权限节点及其授权关系。
-- 保留岗位页面、稳定能力节点和 t_position 数据，先删授权再删旧权限节点。

DELETE role_menu
FROM `t_role_menu` role_menu
JOIN `t_menu` legacy_menu ON legacy_menu.`menu_id` = role_menu.`menu_id`
WHERE legacy_menu.`api_perms` LIKE 'system:position:%'
   OR legacy_menu.`web_perms` LIKE 'system:position:%';

DELETE FROM `t_menu`
WHERE `api_perms` LIKE 'system:position:%'
   OR `web_perms` LIKE 'system:position:%';
