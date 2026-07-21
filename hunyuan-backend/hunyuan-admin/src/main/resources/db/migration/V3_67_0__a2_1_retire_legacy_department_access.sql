-- A2.1 组织目录兼容入口退役：先补齐新能力授权，再删除旧菜单和旧权限码。

-- 将仍挂在旧部门页面上的角色迁移到组织目录读取能力。
INSERT INTO `t_role_menu` (`role_id`, `menu_id`)
SELECT legacy_grant.`role_id`, target_menu.`menu_id`
FROM `t_role_menu` legacy_grant
JOIN `t_menu` legacy_menu ON legacy_menu.`menu_id` = legacy_grant.`menu_id`
JOIN `t_menu` target_menu
  ON target_menu.`path` = '/organization/directory'
 AND target_menu.`api_perms` = 'organization.department.read'
LEFT JOIN `t_role_menu` existing
  ON existing.`role_id` = legacy_grant.`role_id`
 AND existing.`menu_id` = target_menu.`menu_id`
WHERE legacy_menu.`path` = '/organization/department'
  AND existing.`role_menu_id` IS NULL;

-- 员工页面依赖组织目录新读取接口，只补读取能力，不扩大写权限。
INSERT INTO `t_role_menu` (`role_id`, `menu_id`)
SELECT employee_grant.`role_id`, target_menu.`menu_id`
FROM `t_role_menu` employee_grant
JOIN `t_menu` employee_menu ON employee_menu.`menu_id` = employee_grant.`menu_id`
JOIN `t_menu` target_menu
  ON target_menu.`path` = '/organization/directory'
 AND target_menu.`api_perms` = 'organization.department.read'
LEFT JOIN `t_role_menu` existing
  ON existing.`role_id` = employee_grant.`role_id`
 AND existing.`menu_id` = target_menu.`menu_id`
WHERE employee_menu.`path` = '/organization/employee'
  AND existing.`role_menu_id` IS NULL;

-- 将旧动作能力逐项映射到组织目录新能力，保证已有角色权限不丢失。
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
  ON existing.`role_id` = legacy_grant.`role_id`
 AND existing.`menu_id` = target_menu.`menu_id`
WHERE existing.`role_menu_id` IS NULL;

-- 先删除角色关联，再物理删除旧页面和旧权限菜单。
DELETE role_menu
FROM `t_role_menu` role_menu
JOIN `t_menu` legacy_menu ON legacy_menu.`menu_id` = role_menu.`menu_id`
WHERE legacy_menu.`path` = '/organization/department'
   OR legacy_menu.`api_perms` LIKE 'system:department:%'
   OR legacy_menu.`web_perms` LIKE 'system:department:%';

DELETE FROM `t_menu`
WHERE `path` = '/organization/department'
   OR `api_perms` LIKE 'system:department:%'
   OR `web_perms` LIKE 'system:department:%';
