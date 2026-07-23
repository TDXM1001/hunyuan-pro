-- A3.4 P3：通知菜单退役后，清理已删除空父菜单 138 遗留的角色授权。
-- 仅当菜单 138 已不存在时删除授权；若其仍承载其他子节点，则保留原有授权关系。
DELETE role_menu
FROM `t_role_menu` role_menu
LEFT JOIN `t_menu` menu ON menu.`menu_id` = role_menu.`menu_id`
WHERE role_menu.`menu_id` = 138
  AND menu.`menu_id` IS NULL;
