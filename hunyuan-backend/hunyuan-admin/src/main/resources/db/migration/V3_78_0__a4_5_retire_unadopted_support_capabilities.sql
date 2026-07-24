-- A4.5：帮助、反馈和数据追踪没有现行前端消费者或平台责任人，按评估结论完成退役。
-- 开发库中的历史内容已在迁移前独立备份；事务邮件及其模板不属于本次退役范围。

DELETE role_menu
FROM `t_role_menu` role_menu
JOIN `t_menu` menu ON menu.`menu_id` = role_menu.`menu_id`
WHERE menu.`path` IN (
        '/help-doc/help-doc-manage-list',
        '/feedback/feedback-list'
    )
   OR menu.`component` IN (
        '/support/help-doc/management/help-doc-manage-list.vue',
        '/support/feedback/feedback-list.vue'
    )
   OR menu.`api_perms` LIKE 'support:helpDoc:%'
   OR menu.`api_perms` LIKE 'support:helpDocCatalog:%'
   OR menu.`web_perms` LIKE 'support:helpDoc:%'
   OR menu.`web_perms` LIKE 'support:helpDocCatalog:%';

-- 先删除功能权限，再删除指向已不存在前端组件的页面菜单。
DELETE FROM `t_menu`
WHERE `api_perms` LIKE 'support:helpDoc:%'
   OR `api_perms` LIKE 'support:helpDocCatalog:%'
   OR `web_perms` LIKE 'support:helpDoc:%'
   OR `web_perms` LIKE 'support:helpDocCatalog:%';

DELETE FROM `t_menu`
WHERE `path` IN (
        '/help-doc/help-doc-manage-list',
        '/feedback/feedback-list'
    )
   OR `component` IN (
        '/support/help-doc/management/help-doc-manage-list.vue',
        '/support/feedback/feedback-list.vue'
    );

DELETE FROM `t_code_generator_config`
WHERE `table_name` IN (
    't_help_doc',
    't_help_doc_catalog',
    't_help_doc_relation',
    't_help_doc_view_record',
    't_feedback',
    't_data_tracer'
);

-- 从属表先于主表删除，不能依赖当前数据库中不存在的声明式外键级联。
DROP TABLE IF EXISTS `t_help_doc_view_record`;
DROP TABLE IF EXISTS `t_help_doc_relation`;
DROP TABLE IF EXISTS `t_help_doc`;
DROP TABLE IF EXISTS `t_help_doc_catalog`;
DROP TABLE IF EXISTS `t_feedback`;
DROP TABLE IF EXISTS `t_data_tracer`;
