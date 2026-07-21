-- Platform bootstrap seed. Credentials and environment-specific values do not belong here.

ALTER TABLE `t_config`
  ADD UNIQUE KEY `uk_config_key` (`config_key`);

ALTER TABLE `t_employee`
  ADD UNIQUE KEY `uk_employee_login_name` (`login_name`);

ALTER TABLE `t_role_menu`
  ADD UNIQUE KEY `uk_role_menu_role_menu` (`role_id`, `menu_id`);

ALTER TABLE `t_role_data_scope`
  ADD UNIQUE KEY `uk_role_data_scope_type` (`role_id`, `data_scope_type`);

ALTER TABLE `t_dict_data`
  ADD UNIQUE KEY `uk_dict_data_dict_value` (`dict_id`, `data_value`);

CREATE TABLE `t_bootstrap_audit` (
  `bootstrap_audit_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '审计ID',
  `bootstrap_type` varchar(50) NOT NULL COMMENT 'bootstrap类型',
  `subject` varchar(100) NOT NULL COMMENT '非敏感操作对象',
  `status` varchar(30) NOT NULL COMMENT '执行状态',
  `source` varchar(30) NOT NULL COMMENT '配置来源',
  `detail` varchar(500) DEFAULT NULL COMMENT '不含凭据的执行说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`bootstrap_audit_id`),
  UNIQUE KEY `uk_bootstrap_event` (`bootstrap_type`, `subject`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='平台初始化审计';

-- A blank super password disables the legacy universal-password path.
INSERT INTO `t_config` (`config_name`, `config_key`, `config_value`, `remark`)
SELECT '万能密码', 'super_password', '', '平台 seed 默认禁用万能密码'
WHERE NOT EXISTS (
  SELECT 1 FROM `t_config` WHERE `config_key` = 'super_password'
);

INSERT INTO `t_config` (`config_name`, `config_key`, `config_value`, `remark`)
SELECT '三级等保', 'level3_protect_config',
       '{"fileDetectFlag":true,"loginActiveTimeoutMinutes":30,"loginFailLockMinutes":30,"loginFailMaxTimes":5,"maxUploadFileSizeMb":30,"passwordComplexityEnabled":true,"regularChangePasswordMonths":3,"regularChangePasswordNotAllowRepeatTimes":5,"twoFactorLoginEnabled":false}',
       '平台安全基线；环境投产前应按组织制度复核'
WHERE NOT EXISTS (
  SELECT 1 FROM `t_config` WHERE `config_key` = 'level3_protect_config'
);

-- The initial administrator needs an organization anchor, but existing organizations are never overwritten.
INSERT INTO `t_department` (`department_name`, `manager_id`, `parent_id`, `sort`)
SELECT '平台管理', NULL, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM `t_department`);

-- Only dictionaries with stable platform semantics are seeded. Demo/product dictionaries are excluded.
INSERT INTO `t_dict` (`dict_name`, `dict_code`, `remark`, `disabled_flag`)
VALUES
  ('系统性别', 'SYS_GENDER', '平台基础字典：员工性别', 0),
  ('启用状态', 'SYS_DISABLED_FLAG', '平台基础字典：0启用，1禁用', 0)
ON DUPLICATE KEY UPDATE
  `dict_name` = VALUES(`dict_name`),
  `remark` = VALUES(`remark`);

SET @dict_gender = (SELECT `dict_id` FROM `t_dict` WHERE `dict_code` = 'SYS_GENDER');
SET @dict_disabled = (SELECT `dict_id` FROM `t_dict` WHERE `dict_code` = 'SYS_DISABLED_FLAG');

INSERT INTO `t_dict_data`
  (`dict_id`, `data_value`, `data_label`, `data_style`, `remark`, `sort_order`, `disabled_flag`)
VALUES
  (@dict_gender, '0', '未知', 'info', '与 GenderEnum.UNKNOWN 对齐', 0, 0),
  (@dict_gender, '1', '男', 'primary', '与 GenderEnum.MAN 对齐', 2, 0),
  (@dict_gender, '2', '女', 'success', '与 GenderEnum.WOMAN 对齐', 1, 0),
  (@dict_disabled, '0', '启用', 'success', '与 disabled_flag=0 对齐', 1, 0),
  (@dict_disabled, '1', '禁用', 'danger', '与 disabled_flag=1 对齐', 0, 0)
ON DUPLICATE KEY UPDATE
  `data_label` = VALUES(`data_label`),
  `data_style` = VALUES(`data_style`),
  `remark` = VALUES(`remark`),
  `sort_order` = VALUES(`sort_order`);

-- A temporary semantic catalog lets this migration converge both empty and existing v3.64 databases.
CREATE TEMPORARY TABLE `tmp_platform_menu_seed` (
  `seed_key` varchar(80) NOT NULL,
  `parent_key` varchar(80) DEFAULT NULL,
  `menu_name` varchar(200) NOT NULL,
  `menu_type` int(11) NOT NULL,
  `sort_order` int(11) NOT NULL,
  `path` varchar(255) DEFAULT NULL,
  `component` varchar(255) DEFAULT NULL,
  `api_perms` varchar(5000) DEFAULT NULL,
  `icon` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`seed_key`)
);

INSERT INTO `tmp_platform_menu_seed`
  (`seed_key`, `parent_key`, `menu_name`, `menu_type`, `sort_order`, `path`, `component`, `api_perms`, `icon`)
VALUES
  ('org', NULL, '组织架构', 1, 10, '/organization', NULL, NULL, 'UserSwitchOutlined'),
  ('settings', NULL, '系统设置', 1, 20, '/setting', NULL, NULL, 'SettingOutlined'),
  ('security', NULL, '安全治理', 1, 30, '/security', NULL, NULL, 'SafetyCertificateOutlined'),

  ('department', 'org', '部门管理', 2, 10, '/organization/department', '/system/department/department-list.vue', NULL, 'ApartmentOutlined'),
  ('position', 'org', '职务管理', 2, 20, '/organization/position', '/system/position/position-list.vue', NULL, 'ApartmentOutlined'),
  ('employee', 'org', '员工管理', 2, 30, '/organization/employee', '/system/employee/index.vue', NULL, 'AuditOutlined'),
  ('role', 'org', '角色管理', 2, 40, '/organization/role', '/system/role/index.vue', NULL, 'SlidersOutlined'),

  ('menu', 'settings', '菜单管理', 2, 10, '/menu/list', '/system/menu/menu-list.vue', NULL, 'CopyOutlined'),
  ('config', 'settings', '参数配置', 2, 20, '/config/config-list', '/support/config/config-list.vue', NULL, 'AntDesignOutlined'),
  ('dict', 'settings', '数据字典', 2, 30, '/setting/dict', '/support/dict/index.vue', NULL, 'BarcodeOutlined'),

  ('level3', 'security', '安全基线设置', 2, 10, '/support/level3protect/level3-protect-config-index', '/support/level3protect/level3-protect-config-index.vue', NULL, 'SafetyOutlined'),
  ('login_fail', 'security', '登录失败锁定', 2, 20, '/support/login-fail', '/support/login-fail/login-fail-list.vue', NULL, 'LockOutlined'),
  ('login_log', 'security', '登录登出记录', 2, 30, '/support/login-log/login-log-list', '/support/login-log/login-log-list.vue', NULL, 'LoginOutlined'),
  ('operate_log', 'security', '用户操作记录', 2, 40, '/support/operate-log/operate-log-list', '/support/operate-log/operate-log-list.vue', NULL, 'VideoCameraOutlined'),

  ('department_add', 'department', '新增部门', 3, 10, NULL, NULL, 'system:department:add', NULL),
  ('department_update', 'department', '编辑部门', 3, 20, NULL, NULL, 'system:department:update', NULL),
  ('department_delete', 'department', '删除部门', 3, 30, NULL, NULL, 'system:department:delete', NULL),

  ('position_add', 'position', '新增职务', 3, 10, NULL, NULL, 'system:position:add', NULL),
  ('position_update', 'position', '编辑职务', 3, 20, NULL, NULL, 'system:position:update', NULL),
  ('position_delete', 'position', '删除职务', 3, 30, NULL, NULL, 'system:position:delete', NULL),

  ('employee_add', 'employee', '新增员工', 3, 10, NULL, NULL, 'system:employee:add', NULL),
  ('employee_update', 'employee', '编辑员工', 3, 20, NULL, NULL, 'system:employee:update', NULL),
  ('employee_disabled', 'employee', '启用停用员工', 3, 30, NULL, NULL, 'system:employee:disabled', NULL),
  ('employee_department', 'employee', '调整员工部门', 3, 40, NULL, NULL, 'system:employee:department:update', NULL),
  ('employee_password', 'employee', '重置员工密码', 3, 50, NULL, NULL, 'system:employee:password:reset', NULL),
  ('employee_delete', 'employee', '删除员工', 3, 60, NULL, NULL, 'system:employee:delete', NULL),

  ('role_add', 'role', '新增角色', 3, 10, NULL, NULL, 'system:role:add', NULL),
  ('role_update', 'role', '编辑角色', 3, 20, NULL, NULL, 'system:role:update', NULL),
  ('role_delete', 'role', '删除角色', 3, 30, NULL, NULL, 'system:role:delete', NULL),
  ('role_scope', 'role', '更新数据范围', 3, 40, NULL, NULL, 'system:role:dataScope:update', NULL),
  ('role_employee_add', 'role', '添加角色员工', 3, 50, NULL, NULL, 'system:role:employee:add', NULL),
  ('role_employee_delete', 'role', '移除角色员工', 3, 60, NULL, NULL, 'system:role:employee:delete', NULL),
  ('role_employee_batch_delete', 'role', '批量移除角色员工', 3, 70, NULL, NULL, 'system:role:employee:batch:delete', NULL),
  ('role_menu_update', 'role', '更新角色权限', 3, 80, NULL, NULL, 'system:role:menu:update', NULL),

  ('menu_add', 'menu', '新增菜单', 3, 10, NULL, NULL, 'system:menu:add', NULL),
  ('menu_update', 'menu', '编辑菜单', 3, 20, NULL, NULL, 'system:menu:update', NULL),
  ('menu_delete', 'menu', '删除菜单', 3, 30, NULL, NULL, 'system:menu:batchDelete', NULL),

  ('config_query', 'config', '查询参数', 3, 10, NULL, NULL, 'support:config:query', NULL),
  ('config_add', 'config', '新增参数', 3, 20, NULL, NULL, 'support:config:add', NULL),
  ('config_update', 'config', '编辑参数', 3, 30, NULL, NULL, 'support:config:update', NULL),

  ('dict_query', 'dict', '查询字典', 3, 10, NULL, NULL, 'support:dict:query', NULL),
  ('dict_add', 'dict', '新增字典', 3, 20, NULL, NULL, 'support:dict:add', NULL),
  ('dict_update', 'dict', '编辑字典', 3, 30, NULL, NULL, 'support:dict:update', NULL),
  ('dict_delete', 'dict', '删除字典', 3, 40, NULL, NULL, 'support:dict:delete', NULL),
  ('dict_disabled', 'dict', '启用停用字典', 3, 50, NULL, NULL, 'support:dict:updateDisabled', NULL),
  ('dict_data_query', 'dict', '查询字典项', 3, 60, NULL, NULL, 'support:dictData:query', NULL),
  ('dict_data_add', 'dict', '新增字典项', 3, 70, NULL, NULL, 'support:dictData:add', NULL),
  ('dict_data_update', 'dict', '编辑字典项', 3, 80, NULL, NULL, 'support:dictData:update', NULL),
  ('dict_data_delete', 'dict', '删除字典项', 3, 90, NULL, NULL, 'support:dictData:delete', NULL),
  ('dict_data_disabled', 'dict', '启用停用字典项', 3, 100, NULL, NULL, 'support:dictData:updateDisabled', NULL),

  ('level3_query', 'level3', '查看安全基线', 3, 10, NULL, NULL, 'support:protect:level3:query', NULL),
  ('level3_update', 'level3', '更新安全基线', 3, 20, NULL, NULL, 'support:protect:level3:update', NULL),
  ('login_fail_query', 'login_fail', '查询登录锁定', 3, 10, NULL, NULL, 'support:protect:loginFail:query', NULL),
  ('login_fail_delete', 'login_fail', '清除登录锁定', 3, 20, NULL, NULL, 'support:protect:loginFail:delete', NULL),
  ('login_log_query', 'login_log', '查询登录日志', 3, 10, NULL, NULL, 'support:loginLog:query', NULL),
  ('operate_log_query', 'operate_log', '查询操作日志', 3, 10, NULL, NULL, 'support:operateLog:query', NULL),
  ('operate_log_detail', 'operate_log', '查看操作详情', 3, 20, NULL, NULL, 'support:operateLog:detail', NULL);

CREATE TEMPORARY TABLE `tmp_platform_menu_resolved` (
  `seed_key` varchar(80) NOT NULL,
  `menu_id` bigint(20) NOT NULL,
  PRIMARY KEY (`seed_key`),
  UNIQUE KEY `uk_tmp_menu_id` (`menu_id`)
);

INSERT INTO `t_menu`
  (`menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`)
SELECT s.`menu_name`, s.`menu_type`, 0, s.`sort_order`, s.`path`, s.`component`, NULL, NULL, NULL, s.`icon`, 0, 0, 1, 0, 0, 0
FROM `tmp_platform_menu_seed` s
WHERE s.`menu_type` = 1
  AND NOT EXISTS (
    SELECT 1 FROM `t_menu` m
    WHERE m.`parent_id` = 0 AND m.`menu_type` = 1 AND m.`menu_name` = s.`menu_name`
  );

INSERT INTO `tmp_platform_menu_resolved` (`seed_key`, `menu_id`)
SELECT s.`seed_key`, MIN(m.`menu_id`)
FROM `tmp_platform_menu_seed` s
JOIN `t_menu` m ON m.`parent_id` = 0 AND m.`menu_type` = 1 AND m.`menu_name` = s.`menu_name`
WHERE s.`menu_type` = 1
GROUP BY s.`seed_key`;

INSERT INTO `t_menu`
  (`menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`)
SELECT s.`menu_name`, s.`menu_type`, p.`menu_id`, s.`sort_order`, s.`path`, s.`component`, NULL, NULL, NULL, s.`icon`, 0, 0, 1, 0, 0, 0
FROM `tmp_platform_menu_seed` s
JOIN `tmp_platform_menu_resolved` p ON p.`seed_key` = s.`parent_key`
WHERE s.`menu_type` = 2
  AND NOT EXISTS (
    SELECT 1 FROM `t_menu` m WHERE m.`menu_type` = 2 AND m.`path` = s.`path`
  );

INSERT INTO `tmp_platform_menu_resolved` (`seed_key`, `menu_id`)
SELECT s.`seed_key`, MIN(m.`menu_id`)
FROM `tmp_platform_menu_seed` s
JOIN `t_menu` m ON m.`menu_type` = 2 AND m.`path` = s.`path`
WHERE s.`menu_type` = 2
GROUP BY s.`seed_key`;

INSERT INTO `t_menu`
  (`menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`)
SELECT s.`menu_name`, s.`menu_type`, p.`menu_id`, s.`sort_order`, NULL, NULL, 1, s.`api_perms`, s.`api_perms`, NULL, 0, 0, 1, 0, 0, 0
FROM `tmp_platform_menu_seed` s
JOIN `tmp_platform_menu_resolved` p ON p.`seed_key` = s.`parent_key`
WHERE s.`menu_type` = 3
  AND NOT EXISTS (
    SELECT 1 FROM `t_menu` m WHERE m.`menu_type` = 3 AND m.`api_perms` = s.`api_perms`
  );

INSERT INTO `tmp_platform_menu_resolved` (`seed_key`, `menu_id`)
SELECT s.`seed_key`, MIN(m.`menu_id`)
FROM `tmp_platform_menu_seed` s
JOIN `t_menu` m ON m.`menu_type` = 3 AND m.`api_perms` = s.`api_perms`
WHERE s.`menu_type` = 3
GROUP BY s.`seed_key`;

CREATE TEMPORARY TABLE `tmp_platform_parent_resolved` (
  `seed_key` varchar(80) NOT NULL,
  `menu_id` bigint(20) NOT NULL,
  PRIMARY KEY (`seed_key`)
);

INSERT INTO `tmp_platform_parent_resolved` (`seed_key`, `menu_id`)
SELECT `seed_key`, `menu_id`
FROM `tmp_platform_menu_resolved`;

UPDATE `t_menu` m
JOIN `tmp_platform_menu_resolved` r ON r.`menu_id` = m.`menu_id`
JOIN `tmp_platform_menu_seed` s ON s.`seed_key` = r.`seed_key`
LEFT JOIN `tmp_platform_parent_resolved` p ON p.`seed_key` = s.`parent_key`
SET m.`menu_name` = s.`menu_name`,
    m.`menu_type` = s.`menu_type`,
    m.`parent_id` = COALESCE(p.`menu_id`, 0),
    m.`sort` = s.`sort_order`,
    m.`path` = s.`path`,
    m.`component` = s.`component`,
    m.`perms_type` = CASE WHEN s.`menu_type` = 3 THEN 1 ELSE NULL END,
    m.`api_perms` = s.`api_perms`,
    m.`web_perms` = s.`api_perms`,
    m.`icon` = s.`icon`,
    m.`frame_flag` = 0,
    m.`frame_url` = NULL,
    m.`cache_flag` = 0,
    m.`visible_flag` = 1,
    m.`disabled_flag` = 0,
    m.`deleted_flag` = 0,
    m.`update_user_id` = 0;

INSERT INTO `t_role` (`role_name`, `role_code`, `remark`)
VALUES ('平台管理员', 'platform_admin', '平台 seed 内置角色；初始管理员仍以 administrator_flag 作为最终兜底')
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `remark` = VALUES(`remark`);

SET @platform_admin_role_id = (
  SELECT `role_id` FROM `t_role` WHERE `role_code` = 'platform_admin'
);

INSERT INTO `t_role_data_scope` (`data_scope_type`, `view_type`, `role_id`)
VALUES (1, 10, @platform_admin_role_id)
ON DUPLICATE KEY UPDATE `view_type` = VALUES(`view_type`);

INSERT INTO `t_role_menu` (`role_id`, `menu_id`)
SELECT @platform_admin_role_id, r.`menu_id`
FROM `tmp_platform_menu_resolved` r
LEFT JOIN `t_role_menu` rm
  ON rm.`role_id` = @platform_admin_role_id AND rm.`menu_id` = r.`menu_id`
WHERE rm.`role_menu_id` IS NULL;

DROP TEMPORARY TABLE `tmp_platform_menu_resolved`;
DROP TEMPORARY TABLE `tmp_platform_parent_resolved`;
DROP TEMPORARY TABLE `tmp_platform_menu_seed`;
