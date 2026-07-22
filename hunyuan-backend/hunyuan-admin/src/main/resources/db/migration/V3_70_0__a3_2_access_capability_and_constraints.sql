-- A3.2 访问控制稳定能力目录、旧授权迁移与数据库约束守卫。

-- 先清理历史重复数据，再在约束缺失时幂等补建。
DELETE duplicate_role_menu
FROM `t_role_menu` duplicate_role_menu
JOIN `t_role_menu` retained_role_menu
  ON retained_role_menu.`role_id` = duplicate_role_menu.`role_id`
 AND retained_role_menu.`menu_id` = duplicate_role_menu.`menu_id`
 AND retained_role_menu.`role_menu_id` < duplicate_role_menu.`role_menu_id`;

SET @role_menu_unique_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 't_role_menu'
    AND index_name = 'uk_role_menu_role_menu'
);
SET @role_menu_unique_index_sql := IF(
  @role_menu_unique_index_exists = 0,
  'ALTER TABLE `t_role_menu` ADD UNIQUE KEY `uk_role_menu_role_menu` (`role_id`, `menu_id`)',
  'SELECT 1'
);
PREPARE role_menu_unique_index_stmt FROM @role_menu_unique_index_sql;
EXECUTE role_menu_unique_index_stmt;
DEALLOCATE PREPARE role_menu_unique_index_stmt;

DELETE duplicate_scope
FROM `t_role_data_scope` duplicate_scope
JOIN `t_role_data_scope` retained_scope
  ON retained_scope.`role_id` = duplicate_scope.`role_id`
 AND retained_scope.`data_scope_type` = duplicate_scope.`data_scope_type`
 AND retained_scope.`id` < duplicate_scope.`id`;

SET @role_data_scope_unique_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 't_role_data_scope'
    AND index_name = 'uk_role_data_scope_type'
);
SET @role_data_scope_unique_index_sql := IF(
  @role_data_scope_unique_index_exists = 0,
  'ALTER TABLE `t_role_data_scope` ADD UNIQUE KEY `uk_role_data_scope_type` (`role_id`, `data_scope_type`)',
  'SELECT 1'
);
PREPARE role_data_scope_unique_index_stmt FROM @role_data_scope_unique_index_sql;
EXECUTE role_data_scope_unique_index_stmt;
DEALLOCATE PREPARE role_data_scope_unique_index_stmt;

-- 稳定能力节点继续挂在现有角色管理与菜单管理页面下。
CREATE TEMPORARY TABLE `tmp_access_capability_seed` (
  `capability_code` varchar(100) NOT NULL,
  `capability_name` varchar(200) NOT NULL,
  `page_path` varchar(255) NOT NULL,
  `sort_order` int(11) NOT NULL,
  PRIMARY KEY (`capability_code`)
);

INSERT INTO `tmp_access_capability_seed`
  (`capability_code`, `capability_name`, `page_path`, `sort_order`)
VALUES
  ('access.role.read', '查看角色', '/organization/role', 5),
  ('access.role.create', '新增角色', '/organization/role', 10),
  ('access.role.update', '编辑角色', '/organization/role', 20),
  ('access.role.delete', '删除角色', '/organization/role', 30),
  ('access.role.employee.read', '查看角色成员', '/organization/role', 40),
  ('access.role.employee.assign', '分配角色成员', '/organization/role', 50),
  ('access.role.employee.remove', '移除角色成员', '/organization/role', 60),
  ('access.capability.read', '查看角色能力', '/organization/role', 70),
  ('access.capability.grant', '授予角色能力', '/organization/role', 80),
  ('access.data-scope.read', '查看数据范围', '/organization/role', 90),
  ('access.data-scope.update', '更新数据范围', '/organization/role', 100),
  ('access.menu.read', '查看菜单', '/menu/list', 5),
  ('access.menu.create', '新增菜单', '/menu/list', 10),
  ('access.menu.update', '编辑菜单', '/menu/list', 20),
  ('access.menu.delete', '删除菜单', '/menu/list', 30);

INSERT INTO `t_menu`
  (`menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`,
   `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`)
SELECT seed.`capability_name`, 3, page.`menu_id`, seed.`sort_order`, 1,
       seed.`capability_code`, seed.`capability_code`, 1, 0, 0, 0
FROM `tmp_access_capability_seed` seed
JOIN `t_menu` page
  ON page.`menu_type` = 2
 AND page.`path` = seed.`page_path`
 AND page.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1
  FROM `t_menu` existing
  WHERE existing.`menu_type` = 3
    AND existing.`api_perms` = seed.`capability_code`
);

-- 已存在的稳定能力节点也收敛到冻结的名称、父页面和权限类型。
UPDATE `t_menu` capability
JOIN `tmp_access_capability_seed` seed
  ON seed.`capability_code` = capability.`api_perms`
JOIN `t_menu` page
  ON page.`menu_type` = 2
 AND page.`path` = seed.`page_path`
 AND page.`deleted_flag` = 0
SET capability.`menu_name` = seed.`capability_name`,
    capability.`menu_type` = 3,
    capability.`parent_id` = page.`menu_id`,
    capability.`sort` = seed.`sort_order`,
    capability.`perms_type` = 1,
    capability.`web_perms` = seed.`capability_code`,
    capability.`visible_flag` = 1,
    capability.`disabled_flag` = 0,
    capability.`deleted_flag` = 0,
    capability.`update_user_id` = 0;

-- 平台管理员获得完整的稳定访问控制能力。
INSERT INTO `t_role_menu` (`role_id`, `menu_id`)
SELECT platform_role.`role_id`, capability.`menu_id`
FROM `t_role` platform_role
JOIN `t_menu` capability
  ON capability.`api_perms` LIKE 'access.%'
 AND capability.`menu_type` = 3
LEFT JOIN `t_role_menu` existing
  ON existing.`role_id` = platform_role.`role_id`
 AND existing.`menu_id` = capability.`menu_id`
WHERE platform_role.`role_code` = 'platform_admin'
  AND existing.`role_menu_id` IS NULL;

-- 角色页面及相关旧授权分别迁移到最小必要的稳定读取能力。
CREATE TEMPORARY TABLE `tmp_access_read_mapping` (
  `legacy_page_path` varchar(255) DEFAULT NULL,
  `legacy_permission_pattern` varchar(100) DEFAULT NULL,
  `target_permission` varchar(100) NOT NULL
);

INSERT INTO `tmp_access_read_mapping`
  (`legacy_page_path`, `legacy_permission_pattern`, `target_permission`)
VALUES
  ('/organization/role', 'system:role:%', 'access.role.read'),
  ('/organization/role', 'system:role:employee:%', 'access.role.employee.read'),
  ('/organization/role', 'system:role:menu:update', 'access.capability.read'),
  ('/organization/role', 'system:role:dataScope:update', 'access.data-scope.read'),
  ('/menu/list', 'system:menu:%', 'access.menu.read');

INSERT INTO `t_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT legacy_grant.`role_id`, target_menu.`menu_id`
FROM `t_role_menu` legacy_grant
JOIN `t_menu` legacy_menu
  ON legacy_menu.`menu_id` = legacy_grant.`menu_id`
JOIN `tmp_access_read_mapping` mapping
  ON legacy_menu.`path` = mapping.`legacy_page_path`
  OR legacy_menu.`api_perms` LIKE mapping.`legacy_permission_pattern`
JOIN `t_menu` target_menu
  ON target_menu.`api_perms` = mapping.`target_permission`
LEFT JOIN `t_role_menu` existing
  ON existing.`role_id` = legacy_grant.`role_id`
 AND existing.`menu_id` = target_menu.`menu_id`
WHERE existing.`role_menu_id` IS NULL;

-- 旧操作权限按冻结映射复制到稳定能力码，兼容权限留待 P5 退役。
CREATE TEMPORARY TABLE `tmp_access_operation_mapping` (
  `legacy_permission` varchar(100) NOT NULL,
  `target_permission` varchar(100) NOT NULL
);

INSERT INTO `tmp_access_operation_mapping`
  (`legacy_permission`, `target_permission`)
VALUES
  ('system:role:add', 'access.role.create'),
  ('system:role:update', 'access.role.update'),
  ('system:role:delete', 'access.role.delete'),
  ('system:role:employee:add', 'access.role.employee.assign'),
  ('system:role:employee:delete', 'access.role.employee.remove'),
  ('system:role:employee:batch:delete', 'access.role.employee.remove'),
  ('system:role:menu:update', 'access.capability.grant'),
  ('system:role:dataScope:update', 'access.data-scope.update'),
  ('system:menu:add', 'access.menu.create'),
  ('system:menu:update', 'access.menu.update'),
  ('system:menu:batchDelete', 'access.menu.delete');

INSERT INTO `t_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT legacy_grant.`role_id`, target_menu.`menu_id`
FROM `t_role_menu` legacy_grant
JOIN `t_menu` legacy_menu
  ON legacy_menu.`menu_id` = legacy_grant.`menu_id`
JOIN `tmp_access_operation_mapping` mapping
  ON mapping.`legacy_permission` = legacy_menu.`api_perms`
JOIN `t_menu` target_menu
  ON target_menu.`api_perms` = mapping.`target_permission`
LEFT JOIN `t_role_menu` existing
  ON existing.`role_id` = legacy_grant.`role_id`
 AND existing.`menu_id` = target_menu.`menu_id`
WHERE existing.`role_menu_id` IS NULL;

DROP TEMPORARY TABLE `tmp_access_operation_mapping`;
DROP TEMPORARY TABLE `tmp_access_read_mapping`;
DROP TEMPORARY TABLE `tmp_access_capability_seed`;
