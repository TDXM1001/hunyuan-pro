-- BPM 可视化审批规则与业务对象配置中心。
-- 执行前提：已执行 v3.61.0，策略目录菜单 342 和三类策略版本表存在。
-- 兼容影响：新增可空展示列与按钮权限，不改写历史 v1 JSON、digest 或已发布引用。
-- 恢复方式：停用新增入口后可删除菜单 388-391；展示列仅在确认无 v2 草稿后方可删除。

SET @table_name = 't_bpm_candidate_policy_version';
SET @column_name = 'policy_name';
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=@table_name AND column_name=@column_name)=1,'SELECT 1','ALTER TABLE `t_bpm_candidate_policy_version` ADD COLUMN `policy_name` varchar(128) NULL COMMENT ''业务规则名称'' AFTER `policy_digest`'); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_name = 'description'; SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=@table_name AND column_name=@column_name)=1,'SELECT 1','ALTER TABLE `t_bpm_candidate_policy_version` ADD COLUMN `description` varchar(500) NULL COMMENT ''业务说明'' AFTER `policy_name`'); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_name = 'business_summary'; SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=@table_name AND column_name=@column_name)=1,'SELECT 1','ALTER TABLE `t_bpm_candidate_policy_version` ADD COLUMN `business_summary` varchar(1000) NULL COMMENT ''后端生成的业务摘要'' AFTER `description`'); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_name = 'calculated_risk_level'; SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=@table_name AND column_name=@column_name)=1,'SELECT 1','ALTER TABLE `t_bpm_candidate_policy_version` ADD COLUMN `calculated_risk_level` varchar(16) NULL COMMENT ''后端计算风险等级'' AFTER `business_summary`'); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @table_name = 't_bpm_approval_policy_version';
SET @column_name = 'policy_name'; SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=@table_name AND column_name=@column_name)=1,'SELECT 1','ALTER TABLE `t_bpm_approval_policy_version` ADD COLUMN `policy_name` varchar(128) NULL COMMENT ''业务规则名称'' AFTER `policy_digest`'); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_name = 'description'; SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=@table_name AND column_name=@column_name)=1,'SELECT 1','ALTER TABLE `t_bpm_approval_policy_version` ADD COLUMN `description` varchar(500) NULL COMMENT ''业务说明'' AFTER `policy_name`'); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_name = 'business_summary'; SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=@table_name AND column_name=@column_name)=1,'SELECT 1','ALTER TABLE `t_bpm_approval_policy_version` ADD COLUMN `business_summary` varchar(1000) NULL COMMENT ''后端生成的业务摘要'' AFTER `description`'); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_name = 'calculated_risk_level'; SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=@table_name AND column_name=@column_name)=1,'SELECT 1','ALTER TABLE `t_bpm_approval_policy_version` ADD COLUMN `calculated_risk_level` varchar(16) NULL COMMENT ''后端计算风险等级'' AFTER `business_summary`'); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @table_name = 't_bpm_start_visibility_policy_version';
SET @column_name = 'policy_name'; SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=@table_name AND column_name=@column_name)=1,'SELECT 1','ALTER TABLE `t_bpm_start_visibility_policy_version` ADD COLUMN `policy_name` varchar(128) NULL COMMENT ''业务规则名称'' AFTER `policy_digest`'); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_name = 'description'; SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=@table_name AND column_name=@column_name)=1,'SELECT 1','ALTER TABLE `t_bpm_start_visibility_policy_version` ADD COLUMN `description` varchar(500) NULL COMMENT ''业务说明'' AFTER `policy_name`'); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_name = 'business_summary'; SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=@table_name AND column_name=@column_name)=1,'SELECT 1','ALTER TABLE `t_bpm_start_visibility_policy_version` ADD COLUMN `business_summary` varchar(1000) NULL COMMENT ''后端生成的业务摘要'' AFTER `description`'); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @column_name = 'calculated_risk_level'; SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=@table_name AND column_name=@column_name)=1,'SELECT 1','ALTER TABLE `t_bpm_start_visibility_policy_version` ADD COLUMN `calculated_risk_level` varchar(16) NULL COMMENT ''后端计算风险等级'' AFTER `business_summary`'); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `t_menu` SET `menu_name`='审批规则', `update_time`=now() WHERE `menu_id`=342;

INSERT INTO `t_menu` (`menu_id`,`menu_name`,`menu_type`,`parent_id`,`sort`,`perms_type`,`api_perms`,`web_perms`,`context_menu_id`,`frame_flag`,`cache_flag`,`visible_flag`,`disabled_flag`,`deleted_flag`,`create_user_id`,`create_time`,`update_time`)
VALUES
  (388,'保存审批规则草稿',3,342,8,1,'bpm:policy-catalog:save','bpm:policy-catalog:save',342,0,0,1,0,0,1,now(),now()),
  (389,'模拟审批规则',3,342,9,1,'bpm:policy-catalog:simulate','bpm:policy-catalog:simulate',342,0,0,1,0,0,1,now(),now()),
  (390,'查看审批规则技术协议',3,342,10,1,'bpm:policy-catalog:technical','bpm:policy-catalog:technical',342,0,0,1,0,0,1,now(),now()),
  (391,'删除审批规则草稿',3,342,11,1,'bpm:policy-catalog:delete','bpm:policy-catalog:delete',342,0,0,1,0,0,1,now(),now())
ON DUPLICATE KEY UPDATE `menu_name`=VALUES(`menu_name`),`parent_id`=VALUES(`parent_id`),`sort`=VALUES(`sort`),`api_perms`=VALUES(`api_perms`),`web_perms`=VALUES(`web_perms`),`deleted_flag`=0,`disabled_flag`=0,`update_time`=now();

INSERT INTO `t_role_menu` (`role_id`,`menu_id`,`create_time`,`update_time`)
SELECT 1, menu.menu_id, now(), now() FROM `t_menu` menu
WHERE NOT EXISTS (
  SELECT 1 FROM `t_role_menu` existing WHERE existing.role_id=1 AND existing.menu_id=menu.menu_id
)
AND menu.menu_id IN (388,389,390,391);
