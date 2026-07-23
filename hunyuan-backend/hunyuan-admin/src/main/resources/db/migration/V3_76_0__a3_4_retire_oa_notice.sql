-- A3.4 P3：OA 通知未形成正式业务采用，按批准边界完成退役。
-- 历史通知数据已在迁移前独立备份；平台 message、短信和文件能力不属于本迁移。

DELETE role_menu
FROM `t_role_menu` role_menu
JOIN `t_menu` menu ON menu.`menu_id` = role_menu.`menu_id`
WHERE menu.`menu_id` IN (132, 142, 149, 150, 185, 186, 187, 188)
   OR menu.`api_perms` LIKE 'oa:notice:%'
   OR menu.`web_perms` LIKE 'oa:notice:%';

-- 先删除通知操作权限和页面节点，再判断功能 Demo 父菜单是否已经为空。
DELETE FROM `t_menu`
WHERE `api_perms` LIKE 'oa:notice:%'
   OR `web_perms` LIKE 'oa:notice:%';

DELETE FROM `t_menu`
WHERE `menu_id` IN (142, 149, 150, 132);

DELETE parent
FROM `t_menu` parent
LEFT JOIN `t_menu` child ON child.`parent_id` = parent.`menu_id`
WHERE parent.`menu_id` = 138
  AND child.`menu_id` IS NULL;

DELETE FROM `t_code_generator_config`
WHERE `table_name` IN (
    't_notice',
    't_notice_type',
    't_notice_view_record',
    't_notice_visible_range'
);

-- type = 2 只对应已经退役的 OA 通知追踪类型。
DELETE FROM `t_data_tracer`
WHERE `type` = 2;

-- 可见范围和查看记录从属于通知，必须先于通知主表和类型表删除。
DROP TABLE IF EXISTS `t_notice_visible_range`;
DROP TABLE IF EXISTS `t_notice_view_record`;
DROP TABLE IF EXISTS `t_notice`;
DROP TABLE IF EXISTS `t_notice_type`;
