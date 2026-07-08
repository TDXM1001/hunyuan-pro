-- BPM 员工运行时菜单与页面路由
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `icon`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_time`)
VALUES
  (316, '可发起流程', 2, 308, 8, '/system/bpm/runtime/startable-list', '/system/bpm/runtime/startable-list.vue', 1, 'ep:promotion', 1, 0, 0, 1, now(), now()),
  (317, '我的申请', 2, 308, 9, '/system/bpm/runtime/my-instance-list', '/system/bpm/runtime/my-instance-list.vue', 1, 'ep:document-copy', 1, 0, 0, 1, now(), now()),
  (318, '我的待办', 2, 308, 10, '/system/bpm/runtime/my-todo-list', '/system/bpm/runtime/my-todo-list.vue', 1, 'ep:checked', 1, 0, 0, 1, now(), now()),
  (319, '我的已办', 2, 308, 11, '/system/bpm/runtime/my-done-list', '/system/bpm/runtime/my-done-list.vue', 1, 'ep:finished', 1, 0, 0, 1, now(), now())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`),
  `menu_type` = VALUES(`menu_type`),
  `parent_id` = VALUES(`parent_id`),
  `sort` = VALUES(`sort`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `perms_type` = VALUES(`perms_type`),
  `icon` = VALUES(`icon`),
  `visible_flag` = VALUES(`visible_flag`),
  `disabled_flag` = VALUES(`disabled_flag`),
  `deleted_flag` = VALUES(`deleted_flag`),
  `update_time` = now();
