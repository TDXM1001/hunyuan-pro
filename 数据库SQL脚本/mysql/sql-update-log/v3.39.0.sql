-- BPM P1 集成监控菜单与接口权限
-- 背景：v3.38.0 已落库集成可靠性记录表，代码侧已提供回调/命令记录页面和接口。
-- 目标：把页面和 bpm:integration:* 权限接入后台菜单权限体系，避免运行时 404/403。

INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
VALUES
  (321, '集成监控', 1, 308, 13, '/system/bpm/integration', NULL, 1, NULL, NULL, 'ep:monitor', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (322, '回调记录列表', 2, 321, 1, '/system/bpm/integration/callback-record-list', '/system/bpm/integration/callback-record-list.vue', 1, NULL, NULL, 'ep:connection', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (323, '命令记录列表', 2, 321, 2, '/system/bpm/integration/command-record-list', '/system/bpm/integration/command-record-list.vue', 1, NULL, NULL, 'ep:operation', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (324, '查询集成记录', 3, 321, 1, NULL, NULL, 1, 'bpm:integration:query', 'bpm:integration:query', NULL, 321, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (325, '更新集成记录', 3, 321, 2, NULL, NULL, 1, 'bpm:integration:update', 'bpm:integration:update', NULL, 321, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`),
  `menu_type` = VALUES(`menu_type`),
  `parent_id` = VALUES(`parent_id`),
  `sort` = VALUES(`sort`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `perms_type` = VALUES(`perms_type`),
  `api_perms` = VALUES(`api_perms`),
  `web_perms` = VALUES(`web_perms`),
  `icon` = VALUES(`icon`),
  `context_menu_id` = VALUES(`context_menu_id`),
  `frame_flag` = VALUES(`frame_flag`),
  `frame_url` = VALUES(`frame_url`),
  `cache_flag` = VALUES(`cache_flag`),
  `visible_flag` = VALUES(`visible_flag`),
  `disabled_flag` = VALUES(`disabled_flag`),
  `deleted_flag` = VALUES(`deleted_flag`),
  `update_user_id` = VALUES(`update_user_id`),
  `update_time` = now();

INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, m.`menu_id`, now(), now()
FROM `t_menu` m
WHERE m.`menu_id` IN (321, 322, 323, 324, 325)
  AND m.`deleted_flag` = 0
  AND m.`disabled_flag` = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `t_role_menu` rm
    WHERE rm.`role_id` = 1
      AND rm.`menu_id` = m.`menu_id`
  );
