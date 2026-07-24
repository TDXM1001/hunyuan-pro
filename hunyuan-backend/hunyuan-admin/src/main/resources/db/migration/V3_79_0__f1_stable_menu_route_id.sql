ALTER TABLE `t_menu`
  ADD COLUMN `route_id` varchar(160) NULL COMMENT '稳定路由标识' AFTER `path`,
  ADD UNIQUE KEY `uk_menu_route_id` (`route_id`);

UPDATE `t_menu`
SET `route_id` = CASE `menu_id`
  WHEN 219 THEN 'organization.department.directory'
  WHEN 228 THEN 'organization.position.directory'
  WHEN 46 THEN 'identity.employee.management'
  WHEN 76 THEN 'access.role.management'
  WHEN 26 THEN 'access.menu.management'
  ELSE `route_id`
END
WHERE `menu_id` IN (219, 228, 46, 76, 26)
  AND `deleted_flag` = 0;
