-- 已完成稳定路由迁移的有效页面不再存储前端源码 component。
UPDATE `t_menu`
SET `component` = NULL
WHERE `menu_type` = 2
  AND `deleted_flag` = 0
  AND `route_id` IS NOT NULL
  AND TRIM(`route_id`) <> '';
