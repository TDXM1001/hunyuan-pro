-- 为数据脱敏验证页补充管理员查询权限，并纳入安全治理菜单。
INSERT INTO `t_menu`
  (`menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`,
   `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`,
   `disabled_flag`, `deleted_flag`, `create_user_id`)
SELECT '数据脱敏验证', 2, parent.`menu_id`, 50,
       '/support/level3protect/data-masking-list',
       '/support/level3protect/data-masking-list.vue',
       NULL, NULL, NULL, 'SafetyOutlined', 0, 0, 1, 0, 0, 0
FROM `t_menu` parent
WHERE parent.`menu_type` = 1
  AND parent.`path` = '/security'
  AND NOT EXISTS (
    SELECT 1
    FROM `t_menu` existing
    WHERE existing.`menu_type` = 2
      AND existing.`path` = '/support/level3protect/data-masking-list'
  );

INSERT INTO `t_menu`
  (`menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`,
   `api_perms`, `web_perms`, `icon`, `frame_flag`, `cache_flag`, `visible_flag`,
   `disabled_flag`, `deleted_flag`, `create_user_id`)
SELECT '查询数据脱敏验证', 3, page.`menu_id`, 10, NULL, NULL, 1,
       'support:protect:dataMasking:query',
       'support:protect:dataMasking:query',
       NULL, 0, 0, 1, 0, 0, 0
FROM `t_menu` page
WHERE page.`menu_type` = 2
  AND page.`path` = '/support/level3protect/data-masking-list'
  AND NOT EXISTS (
    SELECT 1
    FROM `t_menu` existing
    WHERE existing.`menu_type` = 3
      AND existing.`api_perms` = 'support:protect:dataMasking:query'
  );

-- 平台管理员同时获得页面和查询能力，保证菜单可见性与接口授权一致。
INSERT INTO `t_role_menu` (`role_id`, `menu_id`)
SELECT role.`role_id`, menu.`menu_id`
FROM `t_role` role
JOIN `t_menu` menu
  ON (
       (menu.`menu_type` = 2 AND menu.`path` = '/support/level3protect/data-masking-list')
    OR (menu.`menu_type` = 3 AND menu.`api_perms` = 'support:protect:dataMasking:query')
  )
WHERE role.`role_code` = 'platform_admin'
  AND NOT EXISTS (
    SELECT 1
    FROM `t_role_menu` relation
    WHERE relation.`role_id` = role.`role_id`
      AND relation.`menu_id` = menu.`menu_id`
  );
