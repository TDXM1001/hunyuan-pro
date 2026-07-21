-- A2 organization directory capability and menu migration.

INSERT INTO `t_config` (`config_name`, `config_key`, `config_value`, `remark`)
SELECT '组织目录模块', 'module.organization.directory.enabled', 'true', 'A2 部门目录模块开关；关闭时页面和 API 同时拒绝'
WHERE NOT EXISTS (
  SELECT 1 FROM `t_config` WHERE `config_key` = 'module.organization.directory.enabled'
);

-- Repoint the existing platform department menu to the A2 feature route.
UPDATE `t_menu`
SET `menu_name` = '部门目录',
    `path` = '/organization/directory',
    `component` = '/organization/directory/index.vue',
    `api_perms` = 'organization.department.read',
    `web_perms` = 'organization.department.read',
    `update_user_id` = 0
WHERE `menu_type` = 2
  AND `path` = '/organization/department'
  AND `deleted_flag` = 0;

INSERT INTO `t_menu`
  (`menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`)
SELECT '部门目录', 2, parent.`menu_id`, 10, '/organization/directory', '/organization/directory/index.vue', 1,
       'organization.department.read', 'organization.department.read', 'ApartmentOutlined', 0, 0, 1, 0, 0, 0
FROM `t_menu` parent
WHERE parent.`menu_type` = 1
  AND parent.`path` = '/organization'
  AND NOT EXISTS (
    SELECT 1 FROM `t_menu` existing
    WHERE existing.`menu_type` = 2
      AND existing.`path` = '/organization/directory'
  );

INSERT INTO `t_menu`
  (`menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`)
SELECT source.`menu_name`, 3, page.`menu_id`, source.`sort`, 1, source.`api_perms`, source.`api_perms`, 1, 0, 0, 0
FROM (
  SELECT '新增部门' AS `menu_name`, 10 AS `sort`, 'organization.department.create' AS `api_perms`
  UNION ALL SELECT '编辑部门', 20, 'organization.department.update'
  UNION ALL SELECT '删除部门', 30, 'organization.department.delete'
) source
JOIN `t_menu` page ON page.`menu_type` = 2 AND page.`path` = '/organization/directory'
WHERE NOT EXISTS (
  SELECT 1 FROM `t_menu` existing
  WHERE existing.`menu_type` = 3 AND existing.`api_perms` = source.`api_perms`
);

INSERT INTO `t_role_menu` (`role_id`, `menu_id`)
SELECT role.`role_id`, menu.`menu_id`
FROM `t_role` role
JOIN `t_menu` menu ON menu.`api_perms` IN (
  'organization.department.read',
  'organization.department.create',
  'organization.department.update',
  'organization.department.delete'
)
LEFT JOIN `t_role_menu` existing ON existing.`role_id` = role.`role_id` AND existing.`menu_id` = menu.`menu_id`
WHERE role.`role_code` = 'platform_admin'
  AND existing.`role_menu_id` IS NULL;

-- Preserve legacy role behavior by mapping existing department action grants to A2 capabilities.
INSERT INTO `t_role_menu` (`role_id`, `menu_id`)
SELECT legacy_grant.`role_id`, target_menu.`menu_id`
FROM `t_role_menu` legacy_grant
JOIN `t_menu` legacy_menu ON legacy_menu.`menu_id` = legacy_grant.`menu_id`
JOIN (
  SELECT 'system:department:add' AS `legacy_perm`, 'organization.department.create' AS `target_perm`
  UNION ALL SELECT 'system:department:update', 'organization.department.update'
  UNION ALL SELECT 'system:department:delete', 'organization.department.delete'
) mapping ON mapping.`legacy_perm` = legacy_menu.`api_perms`
JOIN `t_menu` target_menu ON target_menu.`api_perms` = mapping.`target_perm`
LEFT JOIN `t_role_menu` existing
  ON existing.`role_id` = legacy_grant.`role_id` AND existing.`menu_id` = target_menu.`menu_id`
WHERE existing.`role_menu_id` IS NULL;

-- Roles that already owned the department page retain their previous all-department visibility.
INSERT INTO `t_role_data_scope` (`data_scope_type`, `view_type`, `role_id`)
SELECT 2, 10, grants.`role_id`
FROM `t_role_menu` grants
JOIN `t_menu` menu ON menu.`menu_id` = grants.`menu_id`
LEFT JOIN `t_role_data_scope` existing
  ON existing.`role_id` = grants.`role_id` AND existing.`data_scope_type` = 2
WHERE menu.`path` = '/organization/directory'
  AND existing.`id` IS NULL;
