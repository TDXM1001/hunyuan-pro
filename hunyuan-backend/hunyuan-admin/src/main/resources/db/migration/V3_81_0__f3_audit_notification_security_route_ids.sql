-- F3 保留页面统一使用稳定 routeId；component 在 F6 全量验收后统一清空。
UPDATE `t_menu`
SET `route_id` = CASE `menu_id`
  WHEN 143 THEN 'platform.audit.login-log'
  WHEN 81 THEN 'platform.audit.operation-log'
  WHEN 300 THEN 'platform.notification.message'
  WHEN 306 THEN 'platform.notification.sms-template'
  WHEN 307 THEN 'platform.notification.sms-send-log'
  WHEN 250 THEN 'platform.security.baseline-settings'
  WHEN 214 THEN 'platform.security.login-failure'
  WHEN 251 THEN 'platform.security.data-masking-validation'
  ELSE `route_id`
END
WHERE `menu_id` IN (81, 143, 214, 250, 251, 300, 306, 307)
  AND `deleted_flag` = 0;

-- 更新日志没有前端页面和生产消费者，退役菜单及其全部子权限授权。
DELETE relation
FROM `t_role_menu` relation
JOIN `t_menu` menu ON menu.`menu_id` = relation.`menu_id`
WHERE menu.`menu_id` = 152 OR menu.`parent_id` = 152;

UPDATE `t_menu`
SET `deleted_flag` = 1, `disabled_flag` = 1, `visible_flag` = 0
WHERE `menu_id` = 152 OR `parent_id` = 152;
