-- BPM 运行端：我的抄送
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `icon`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_time`)
VALUES
  (320, '我的抄送', 2, 308, 12, '/system/bpm/runtime/my-copy-list', '/system/bpm/runtime/my-copy-list.vue', 1, 'ep:message', 1, 0, 0, 1, now(), now())
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

INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT r.`role_id`, 320, now(), now()
FROM `t_role` r
JOIN `t_menu` m ON m.`menu_id` = 320
WHERE r.`role_code` = 'bpm_runtime_user'
  AND m.`deleted_flag` = 0
  AND m.`disabled_flag` = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `t_role_menu` rm
    WHERE rm.`role_id` = r.`role_id`
      AND rm.`menu_id` = 320
  );
