-- F4 本地页面使用稳定 routeId；Swagger 继续按外链菜单处理。
UPDATE `t_menu`
SET `route_id` = CASE `menu_id`
  WHEN 221 THEN 'platform.runtime.job'
  WHEN 130 THEN 'platform.runtime.serial-number'
  WHEN 133 THEN 'platform.runtime.cache'
  WHEN 117 THEN 'platform.runtime.reload'
  WHEN 215 THEN 'platform.devtools.api-encrypt'
  ELSE `route_id`
END
WHERE `menu_id` IN (117, 130, 133, 215, 221)
  AND `deleted_flag` = 0;

-- 三个悬空或未采用的生产菜单退役；后端内部能力不随菜单删除。
DELETE relation
FROM `t_role_menu` relation
JOIN `t_menu` menu ON menu.`menu_id` = relation.`menu_id`
WHERE menu.`menu_id` IN (85, 151, 206)
   OR menu.`parent_id` IN (85, 151, 206);

UPDATE `t_menu`
SET `deleted_flag` = 1, `disabled_flag` = 1, `visible_flag` = 0
WHERE `menu_id` IN (85, 151, 206)
   OR `parent_id` IN (85, 151, 206);
