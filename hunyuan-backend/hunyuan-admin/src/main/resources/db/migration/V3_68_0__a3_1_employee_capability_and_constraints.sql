-- A3.1 employee capability seed and database invariants.

-- Employee identity uniqueness already exists in V3.65.0 and the baseline.
-- Keep those constraints and make the common directory filter indexable.
SET @employee_directory_state_index_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 't_employee'
    AND index_name = 'idx_employee_directory_state'
);
SET @employee_directory_state_index_sql := IF(
  @employee_directory_state_index_exists = 0,
  'ALTER TABLE `t_employee` ADD KEY `idx_employee_directory_state` (`department_id`, `deleted_flag`, `disabled_flag`)',
  'SELECT 1'
);
PREPARE employee_directory_state_index_stmt FROM @employee_directory_state_index_sql;
EXECUTE employee_directory_state_index_stmt;
DEALLOCATE PREPARE employee_directory_state_index_stmt;

-- New capability nodes live under the existing employee page during compatibility migration.
INSERT INTO `t_menu`
  (`menu_name`, `menu_type`, `parent_id`, `sort`, `perms_type`, `api_perms`, `web_perms`,
   `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`)
SELECT source.`menu_name`, 3, page.`menu_id`, source.`sort_order`, 1,
       source.`api_perms`, source.`api_perms`, 1, 0, 0, 0
FROM (
  SELECT 'Employee read' AS `menu_name`, 5 AS `sort_order`, 'identity.employee.read' AS `api_perms`
  UNION ALL SELECT 'Create employee', 10, 'identity.employee.create'
  UNION ALL SELECT 'Update employee', 20, 'identity.employee.update'
  UNION ALL SELECT 'Enable employee', 30, 'identity.employee.enable'
  UNION ALL SELECT 'Disable employee', 40, 'identity.employee.disable'
  UNION ALL SELECT 'Assign employee department', 50, 'identity.employee.department.assign'
  UNION ALL SELECT 'Delete employee', 60, 'identity.employee.delete'
  UNION ALL SELECT 'Reset employee password', 70, 'identity.employee.password.reset'
) source
JOIN `t_menu` page
  ON page.`menu_type` = 2
 AND page.`path` = '/organization/employee'
 AND page.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1
  FROM `t_menu` existing
  WHERE existing.`menu_type` = 3
    AND existing.`api_perms` = source.`api_perms`
);

-- The platform administrator receives the complete new capability set.
INSERT INTO `t_role_menu` (`role_id`, `menu_id`)
SELECT role.`role_id`, menu.`menu_id`
FROM `t_role` role
JOIN `t_menu` menu
  ON menu.`api_perms` LIKE 'identity.employee.%'
 AND menu.`menu_type` = 3
LEFT JOIN `t_role_menu` existing
  ON existing.`role_id` = role.`role_id`
 AND existing.`menu_id` = menu.`menu_id`
WHERE role.`role_code` = 'platform_admin'
  AND existing.`role_menu_id` IS NULL;

-- Existing employee-page readers retain read access in the new capability namespace.
INSERT INTO `t_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT legacy_grant.`role_id`, target_menu.`menu_id`
FROM `t_role_menu` legacy_grant
JOIN `t_menu` legacy_menu
  ON legacy_menu.`menu_id` = legacy_grant.`menu_id`
JOIN `t_menu` target_menu
  ON target_menu.`api_perms` = 'identity.employee.read'
LEFT JOIN `t_role_menu` existing
  ON existing.`role_id` = legacy_grant.`role_id`
 AND existing.`menu_id` = target_menu.`menu_id`
WHERE (
       legacy_menu.`path` = '/organization/employee'
    OR legacy_menu.`api_perms` LIKE 'system:employee:%'
)
  AND existing.`role_menu_id` IS NULL;

-- Preserve existing action grants while introducing the new stable capability codes.
INSERT INTO `t_role_menu` (`role_id`, `menu_id`)
SELECT legacy_grant.`role_id`, target_menu.`menu_id`
FROM `t_role_menu` legacy_grant
JOIN `t_menu` legacy_menu
  ON legacy_menu.`menu_id` = legacy_grant.`menu_id`
JOIN (
  SELECT 'system:employee:add' AS `legacy_perm`, 'identity.employee.create' AS `target_perm`
  UNION ALL SELECT 'system:employee:update', 'identity.employee.update'
  UNION ALL SELECT 'system:employee:disabled', 'identity.employee.enable'
  UNION ALL SELECT 'system:employee:disabled', 'identity.employee.disable'
  UNION ALL SELECT 'system:employee:department:update', 'identity.employee.department.assign'
  UNION ALL SELECT 'system:employee:password:reset', 'identity.employee.password.reset'
  UNION ALL SELECT 'system:employee:delete', 'identity.employee.delete'
) mapping
  ON mapping.`legacy_perm` = legacy_menu.`api_perms`
JOIN `t_menu` target_menu
  ON target_menu.`api_perms` = mapping.`target_perm`
LEFT JOIN `t_role_menu` existing
  ON existing.`role_id` = legacy_grant.`role_id`
 AND existing.`menu_id` = target_menu.`menu_id`
WHERE existing.`role_menu_id` IS NULL;
