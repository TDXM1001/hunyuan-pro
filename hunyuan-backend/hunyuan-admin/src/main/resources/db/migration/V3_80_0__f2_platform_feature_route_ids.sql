-- F2 已迁移页面优先按稳定路由装配，历史 component 字段仅保留给尚未迁移的菜单。
UPDATE `t_menu`
SET `route_id` = CASE `menu_id`
  WHEN 109 THEN 'platform.configuration.parameters'
  WHEN 110 THEN 'platform.configuration.dictionary'
  WHEN 193 THEN 'platform.file.management'
  ELSE `route_id`
END
WHERE `menu_id` IN (109, 110, 193)
  AND `deleted_flag` = 0;
