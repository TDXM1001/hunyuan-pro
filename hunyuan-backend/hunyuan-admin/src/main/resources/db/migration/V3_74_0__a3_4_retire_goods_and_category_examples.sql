-- A3.4 P1：商品与分类仅服务历史示例，仓库内外消费者均已确认归零。
-- 先移除角色授权、菜单权限和示例字典，再删除示例数据表，避免留下不可达授权与孤立配置。

DELETE role_menu
FROM `t_role_menu` role_menu
JOIN `t_menu` menu ON menu.`menu_id` = role_menu.`menu_id`
WHERE menu.`path` IN ('/goods', '/erp/goods/list', '/erp/catalog/goods', '/erp/catalog/custom')
   OR menu.`api_perms` LIKE 'goods:%'
   OR menu.`web_perms` LIKE 'goods:%'
   OR menu.`api_perms` LIKE 'category:%'
   OR menu.`web_perms` LIKE 'category:%'
   OR menu.`web_perms` LIKE 'custom:category:%';

-- 先删除操作权限，再删除页面和父目录，保持菜单层级清理顺序明确。
DELETE FROM `t_menu`
WHERE `api_perms` LIKE 'goods:%'
   OR `web_perms` LIKE 'goods:%'
   OR `api_perms` LIKE 'category:%'
   OR `web_perms` LIKE 'category:%'
   OR `web_perms` LIKE 'custom:category:%';

DELETE FROM `t_menu`
WHERE `path` IN ('/erp/goods/list', '/erp/catalog/goods', '/erp/catalog/custom');

DELETE FROM `t_menu`
WHERE `path` = '/goods';

-- GOODS_PLACE 只服务商品示例，不属于平台稳定字典。
DELETE dict_data
FROM `t_dict_data` dict_data
JOIN `t_dict` dict ON dict.`dict_id` = dict_data.`dict_id`
WHERE dict.`dict_code` = 'GOODS_PLACE';

DELETE FROM `t_dict`
WHERE `dict_code` = 'GOODS_PLACE';

DELETE FROM `t_code_generator_config`
WHERE `table_name` IN ('t_goods', 't_category');

DROP TABLE IF EXISTS `t_goods`;
DROP TABLE IF EXISTS `t_category`;
