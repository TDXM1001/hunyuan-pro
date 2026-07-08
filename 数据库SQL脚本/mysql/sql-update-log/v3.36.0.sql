-- BPM 运行端账号权限补齐
-- 背景：huke 已有 /app/bpm/my-todo 等运行端接口数据，但登录菜单未包含 /system/bpm/runtime/* 页面。
-- 策略：新增独立 BPM 运行端角色并绑定 huke，避免直接扩大“技术总监”等共享角色权限。
-- 执行后请让 huke 重新登录；若后端仍返回旧菜单，请清理登录/权限缓存或重启后端。

INSERT INTO `t_role` (`role_name`, `role_code`, `remark`, `create_time`, `update_time`)
VALUES ('BPM运行端用户', 'bpm_runtime_user', '允许员工访问 BPM 可发起、我的申请、我的待办、我的已办页面', now(), now())
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `remark` = VALUES(`remark`),
  `update_time` = now();

INSERT INTO `t_role_employee` (`role_id`, `employee_id`, `create_time`, `update_time`)
SELECT r.`role_id`, e.`employee_id`, now(), now()
FROM `t_role` r
JOIN `t_employee` e ON e.`login_name` = 'huke'
WHERE r.`role_code` = 'bpm_runtime_user'
  AND e.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `t_role_employee` re
    WHERE re.`role_id` = r.`role_id`
      AND re.`employee_id` = e.`employee_id`
  );

INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT r.`role_id`, m.`menu_id`, now(), now()
FROM `t_role` r
JOIN `t_menu` m ON m.`menu_id` IN (308, 316, 317, 318, 319)
WHERE r.`role_code` = 'bpm_runtime_user'
  AND m.`deleted_flag` = 0
  AND m.`disabled_flag` = 0
  AND NOT EXISTS (
    SELECT 1
    FROM `t_role_menu` rm
    WHERE rm.`role_id` = r.`role_id`
      AND rm.`menu_id` = m.`menu_id`
  );
