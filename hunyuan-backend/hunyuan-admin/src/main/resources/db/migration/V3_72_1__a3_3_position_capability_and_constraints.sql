-- A3.3 P3：岗位目录稳定能力、旧授权迁移与数据库一致性守卫。

-- 有效岗位名称必须唯一；逻辑删除记录生成 NULL，因此不阻止后续复用名称。
SET @position_active_name_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 't_position'
    AND column_name = 'active_position_name'
);
SET @position_active_name_column_sql := IF(
  @position_active_name_column_exists = 0,
  'ALTER TABLE `t_position` ADD COLUMN `active_position_name` varchar(200) GENERATED ALWAYS AS (CASE WHEN `deleted_flag` = 0 THEN TRIM(`position_name`) ELSE NULL END) STORED',
  'SELECT 1'
);
PREPARE position_active_name_column_stmt FROM @position_active_name_column_sql;
EXECUTE position_active_name_column_stmt;
DEALLOCATE PREPARE position_active_name_column_stmt;

SET @position_active_name_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 't_position'
    AND index_name = 'uk_position_active_name'
);
SET @position_active_name_index_sql := IF(
  @position_active_name_index_exists = 0,
  'ALTER TABLE `t_position` ADD UNIQUE KEY `uk_position_active_name` (`active_position_name`)',
  'SELECT 1'
);
PREPARE position_active_name_index_stmt FROM @position_active_name_index_sql;
EXECUTE position_active_name_index_stmt;
DEALLOCATE PREPARE position_active_name_index_stmt;

-- 岗位目录读取能力由页面节点承载，继续复用现有路由和组件，前端切换留到 P4。
UPDATE `t_menu`
SET `menu_name` = '岗位目录',
    `perms_type` = 1,
    `api_perms` = 'organization.position.read',
    `web_perms` = 'organization.position.read',
    `update_user_id` = 0
WHERE `menu_type` = 2
  AND `path` = '/organization/position'
  AND `deleted_flag` = 0;

CREATE TEMPORARY TABLE `tmp_position_capability_seed` (
  `capability_code` varchar(100) NOT NULL,
  `capability_name` varchar(200) NOT NULL,
  `sort_order` int(11) NOT NULL,
  PRIMARY KEY (`capability_code`)
);

INSERT INTO `tmp_position_capability_seed`
  (`capability_code`, `capability_name`, `sort_order`)
VALUES
  ('organization.position.create', '新增岗位', 10),
  ('organization.position.update', '编辑岗位', 20),
  ('organization.position.delete', '删除岗位', 30);

INSERT INTO `t_menu`
  (`menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`,
   `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`)
SELECT seed.`capability_name`, 3, page.`menu_id`, seed.`sort_order`, 1,
       seed.`capability_code`, seed.`capability_code`, 1, 0, 0, 0
FROM `tmp_position_capability_seed` seed
JOIN `t_menu` page
  ON page.`menu_type` = 2
 AND page.`path` = '/organization/position'
 AND page.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1
  FROM `t_menu` existing
  WHERE existing.`menu_type` = 3
    AND existing.`api_perms` = seed.`capability_code`
);

-- 已存在的稳定能力节点也收敛到冻结的名称、父页面和权限类型。
UPDATE `t_menu` capability
JOIN `tmp_position_capability_seed` seed
  ON seed.`capability_code` = capability.`api_perms`
JOIN `t_menu` page
  ON page.`menu_type` = 2
 AND page.`path` = '/organization/position'
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

-- 平台管理员获得完整岗位目录能力。
INSERT INTO `t_role_menu` (`role_id`, `menu_id`)
SELECT platform_role.`role_id`, capability.`menu_id`
FROM `t_role` platform_role
JOIN `t_menu` capability
  ON capability.`api_perms` LIKE 'organization.position.%'
 AND capability.`menu_type` IN (2, 3)
LEFT JOIN `t_role_menu` existing
  ON existing.`role_id` = platform_role.`role_id`
 AND existing.`menu_id` = capability.`menu_id`
WHERE platform_role.`role_code` = 'platform_admin'
  AND existing.`role_menu_id` IS NULL;

-- 已拥有岗位页面或旧岗位操作权限的角色继续拥有稳定读取能力。
INSERT INTO `t_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT legacy_grant.`role_id`, target_page.`menu_id`
FROM `t_role_menu` legacy_grant
JOIN `t_menu` legacy_menu
  ON legacy_menu.`menu_id` = legacy_grant.`menu_id`
JOIN `t_menu` target_page
  ON target_page.`menu_type` = 2
 AND target_page.`path` = '/organization/position'
 AND target_page.`api_perms` = 'organization.position.read'
LEFT JOIN `t_role_menu` existing
  ON existing.`role_id` = legacy_grant.`role_id`
 AND existing.`menu_id` = target_page.`menu_id`
WHERE (
       legacy_menu.`path` = '/organization/position'
    OR legacy_menu.`api_perms` LIKE 'system:position:%'
)
  AND existing.`role_menu_id` IS NULL;

-- 旧操作权限按冻结映射复制到稳定能力码，旧节点保留至 P5。
CREATE TEMPORARY TABLE `tmp_position_operation_mapping` (
  `legacy_permission` varchar(100) NOT NULL,
  `target_permission` varchar(100) NOT NULL
);

INSERT INTO `tmp_position_operation_mapping`
  (`legacy_permission`, `target_permission`)
VALUES
  ('system:position:add', 'organization.position.create'),
  ('system:position:update', 'organization.position.update'),
  ('system:position:delete', 'organization.position.delete');

INSERT INTO `t_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT legacy_grant.`role_id`, target_menu.`menu_id`
FROM `t_role_menu` legacy_grant
JOIN `t_menu` legacy_menu
  ON legacy_menu.`menu_id` = legacy_grant.`menu_id`
JOIN `tmp_position_operation_mapping` mapping
  ON mapping.`legacy_permission` = legacy_menu.`api_perms`
JOIN `t_menu` target_menu
  ON target_menu.`api_perms` = mapping.`target_permission`
LEFT JOIN `t_role_menu` existing
  ON existing.`role_id` = legacy_grant.`role_id`
 AND existing.`menu_id` = target_menu.`menu_id`
WHERE existing.`role_menu_id` IS NULL;

DROP TEMPORARY TABLE `tmp_position_operation_mapping`;
DROP TEMPORARY TABLE `tmp_position_capability_seed`;
