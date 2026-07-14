-- BPM Graph-only control plane cleanup.
-- 执行前提：已执行 v3.60.0，Graph 草稿、发布与运行链路已可用。
-- 兼容影响：仅调整 BPM 管理端菜单与权限；不删除历史定义表或实例投影数据。
-- 恢复方式：重新启用菜单 312 并恢复所需角色绑定；菜单 311 可改回原名称。

UPDATE `t_menu`
SET `menu_name` = 'Graph 流程设计',
    `path` = '/system/bpm/model',
    `component` = '/system/bpm/model/model-list.vue',
    `visible_flag` = 1,
    `disabled_flag` = 0,
    `deleted_flag` = 0,
    `update_time` = now()
WHERE `menu_id` = 311;

INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
VALUES
  (387, '查询Graph草稿', 3, 311, 9, NULL, NULL, 1, 'bpm:graph-draft:query', 'bpm:graph-draft:query', NULL, 311, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `parent_id` = VALUES(`parent_id`), `sort` = VALUES(`sort`),
  `api_perms` = VALUES(`api_perms`), `web_perms` = VALUES(`web_perms`),
  `context_menu_id` = VALUES(`context_menu_id`), `visible_flag` = VALUES(`visible_flag`),
  `disabled_flag` = VALUES(`disabled_flag`), `deleted_flag` = VALUES(`deleted_flag`), `update_time` = now();

INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, 387, now(), now()
WHERE NOT EXISTS (
  SELECT 1 FROM `t_role_menu` WHERE `role_id` = 1 AND `menu_id` = 387
);

DELETE FROM `t_role_menu` WHERE `menu_id` = 312;

UPDATE `t_menu`
SET `visible_flag` = 0,
    `disabled_flag` = 1,
    `deleted_flag` = 1,
    `update_time` = now()
WHERE `menu_id` = 312;
