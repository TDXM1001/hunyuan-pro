-- A3.4 P2：企业、企业员工关联、银行和发票均为已批准退役的历史 OA 示例能力。
-- 数据已完成独立备份与恢复抽样；本迁移只处理 P2 范围，保留 OA 通知和功能 Demo 父菜单。

DELETE role_menu
FROM `t_role_menu` role_menu
JOIN `t_menu` menu ON menu.`menu_id` = role_menu.`menu_id`
WHERE menu.`menu_id` IN (144, 145)
   OR menu.`api_perms` IN (
        'oa:enterprise:query',
        'oa:enterprise:add',
        'oa:enterprise:update',
        'oa:enterprise:delete',
        'oa:enterprise:detail',
        'oa:enterprise:queryEmployee',
        'oa:enterprise:addEmployee',
        'oa:enterprise:deleteEmployee',
        'oa:bank:query',
        'oa:bank:add',
        'oa:bank:update',
        'oa:bank:delete',
        'oa:invoice:query',
        'oa:invoice:add',
        'oa:invoice:update',
        'oa:invoice:delete'
    )
   OR menu.`web_perms` IN (
        'oa:enterprise:query',
        'oa:enterprise:add',
        'oa:enterprise:update',
        'oa:enterprise:delete',
        'oa:enterprise:detail',
        'oa:enterprise:queryEmployee',
        'oa:enterprise:addEmployee',
        'oa:enterprise:deleteEmployee',
        'oa:bank:query',
        'oa:bank:add',
        'oa:bank:update',
        'oa:bank:delete',
        'oa:invoice:query',
        'oa:invoice:add',
        'oa:invoice:update',
        'oa:invoice:delete'
    );

-- 先删除操作权限，再删除企业页面；功能 Demo 父菜单由 P3 通知退役后另行判断。
DELETE FROM `t_menu`
WHERE `api_perms` IN (
        'oa:enterprise:query',
        'oa:enterprise:add',
        'oa:enterprise:update',
        'oa:enterprise:delete',
        'oa:enterprise:detail',
        'oa:enterprise:queryEmployee',
        'oa:enterprise:addEmployee',
        'oa:enterprise:deleteEmployee',
        'oa:bank:query',
        'oa:bank:add',
        'oa:bank:update',
        'oa:bank:delete',
        'oa:invoice:query',
        'oa:invoice:add',
        'oa:invoice:update',
        'oa:invoice:delete'
    )
   OR `web_perms` IN (
        'oa:enterprise:query',
        'oa:enterprise:add',
        'oa:enterprise:update',
        'oa:enterprise:delete',
        'oa:enterprise:detail',
        'oa:enterprise:queryEmployee',
        'oa:enterprise:addEmployee',
        'oa:enterprise:deleteEmployee',
        'oa:bank:query',
        'oa:bank:add',
        'oa:bank:update',
        'oa:bank:delete',
        'oa:invoice:query',
        'oa:invoice:add',
        'oa:invoice:update',
        'oa:invoice:delete'
    );

DELETE FROM `t_menu`
WHERE `menu_id` IN (144, 145);

DELETE FROM `t_code_generator_config`
WHERE `table_name` IN (
    't_oa_enterprise',
    't_oa_enterprise_employee',
    't_oa_bank',
    't_oa_invoice'
);

-- type = 3 只对应已经退役的 OA 企业追踪类型。
DELETE FROM `t_data_tracer`
WHERE `type` = 3;

-- 从属数据先于企业主表删除，不能依赖当前数据库中不存在的声明式外键级联。
DROP TABLE IF EXISTS `t_oa_bank`;
DROP TABLE IF EXISTS `t_oa_invoice`;
DROP TABLE IF EXISTS `t_oa_enterprise_employee`;
DROP TABLE IF EXISTS `t_oa_enterprise`;
