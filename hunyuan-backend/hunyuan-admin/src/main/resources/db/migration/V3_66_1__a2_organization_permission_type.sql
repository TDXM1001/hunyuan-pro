-- Repair permission typing when the A2 page reused a legacy department menu row.

UPDATE `t_menu`
SET `perms_type` = 1
WHERE `menu_type` IN (2, 3)
  AND (`path` = '/organization/directory'
       OR `api_perms` LIKE 'organization.department.%')
  AND `deleted_flag` = 0;
