-- BPM M1 Graph：发布版本只读检查权限。
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
VALUES
  (341, '查看Graph定义', 3, 311, 17, NULL, NULL, 1, 'bpm:graph-definition:detail', 'bpm:graph-definition:detail', NULL, 311, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `parent_id` = VALUES(`parent_id`), `sort` = VALUES(`sort`),
  `api_perms` = VALUES(`api_perms`), `web_perms` = VALUES(`web_perms`),
  `context_menu_id` = VALUES(`context_menu_id`), `visible_flag` = VALUES(`visible_flag`),
  `disabled_flag` = VALUES(`disabled_flag`), `deleted_flag` = VALUES(`deleted_flag`), `update_time` = now();

INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, menu.`menu_id`, now(), now()
FROM `t_menu` menu
WHERE menu.`menu_id` = 341
  AND menu.`deleted_flag` = 0
  AND menu.`disabled_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `t_role_menu` role_menu
    WHERE role_menu.`role_id` = 1 AND role_menu.`menu_id` = menu.`menu_id`
  );
