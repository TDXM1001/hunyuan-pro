-- 短信菜单目录改为系统设置下的直接页面，避免前端再包一层布局路由
UPDATE `t_menu`
SET
  `parent_id` = 50,
  `sort` = CASE `menu_id`
    WHEN 306 THEN 31
    WHEN 307 THEN 32
    ELSE `sort`
  END,
  `icon` = CASE `menu_id`
    WHEN 306 THEN 'ep:tickets'
    WHEN 307 THEN 'ep:list'
    ELSE `icon`
  END,
  `update_time` = now()
WHERE `menu_id` IN (306, 307);

-- 继续把按钮权限挂回各自页面，避免目录层收缩后权限上下文丢失
UPDATE `t_menu`
SET
  `parent_id` = 306,
  `context_menu_id` = 306,
  `update_time` = now()
WHERE `menu_id` IN (301, 302, 303);

UPDATE `t_menu`
SET
  `parent_id` = 307,
  `context_menu_id` = 307,
  `update_time` = now()
WHERE `menu_id` = 304;

-- 原短信管理目录只用于承接子页面，扁平化后直接逻辑删除，避免继续生成额外路由壳
UPDATE `t_menu`
SET
  `deleted_flag` = 1,
  `disabled_flag` = 1,
  `visible_flag` = 0,
  `update_time` = now()
WHERE `menu_id` = 305;
