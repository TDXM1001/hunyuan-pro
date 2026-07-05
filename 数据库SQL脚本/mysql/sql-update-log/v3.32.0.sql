-- 短信管理父菜单与两个子页面菜单
INSERT INTO `t_menu` (
  `menu_id`,
  `menu_name`,
  `menu_type`,
  `parent_id`,
  `sort`,
  `path`,
  `component`,
  `perms_type`,
  `api_perms`,
  `web_perms`,
  `icon`,
  `context_menu_id`,
  `frame_flag`,
  `frame_url`,
  `cache_flag`,
  `visible_flag`,
  `disabled_flag`,
  `deleted_flag`,
  `create_user_id`,
  `create_time`,
  `update_user_id`,
  `update_time`
)
VALUES
  (305, '短信管理', 1, 50, 31, '/support/sms', NULL, 1, NULL, NULL, 'ep:chat-dot-round', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (306, '短信模板', 2, 305, 1, '/support/sms/template-list', '/support/sms/template-list.vue', 1, NULL, NULL, 'ep:tickets', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (307, '发送日志', 2, 305, 2, '/support/sms/send-log-list', '/support/sms/send-log-list.vue', 1, NULL, NULL, 'ep:list', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`),
  `menu_type` = VALUES(`menu_type`),
  `parent_id` = VALUES(`parent_id`),
  `sort` = VALUES(`sort`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `icon` = VALUES(`icon`),
  `update_time` = now();

-- 归位既有短信模板按钮权限
UPDATE `t_menu`
SET
  `parent_id` = 306,
  `context_menu_id` = 306,
  `update_time` = now()
WHERE `menu_id` IN (301, 302, 303);

-- 归位既有发送日志查询权限
UPDATE `t_menu`
SET
  `parent_id` = 307,
  `context_menu_id` = 307,
  `update_time` = now()
WHERE `menu_id` = 304;
