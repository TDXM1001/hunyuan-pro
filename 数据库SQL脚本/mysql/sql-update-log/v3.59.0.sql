-- BPM M7：运营与治理统一异常队列、处置审计、指标与保留策略。
-- 执行前提：已执行 v3.58.0（M6 配置化业务接入与可靠事件订阅），M4-M6 已产生实例、任务、时间事件、外部等待、命令和回调事实。
-- 兼容影响：仅新增 M7 运营治理表、菜单与权限；不修改 Flowable 表，不改变 M4-M6 运行事实语义。
-- 恢复方式：确认无未关闭运营工单和审计保留要求后，删除 M7 菜单权限并备份后移除新增表。
CREATE TABLE IF NOT EXISTS `t_bpm_operations_case` (
  `operations_case_id` bigint NOT NULL AUTO_INCREMENT,
  `case_code` varchar(64) NOT NULL,
  `source_type` varchar(32) NOT NULL,
  `source_id` bigint NOT NULL,
  `event_id` varchar(128) NULL,
  `instance_id` bigint NULL,
  `definition_id` bigint NULL,
  `graph_definition_version_id` bigint NULL,
  `definition_node_id` varchar(128) NULL,
  `node_name` varchar(128) NULL,
  `organization_id` bigint NULL,
  `assignee_employee_id` bigint NULL,
  `business_type` varchar(128) NULL,
  `business_id` bigint NULL,
  `business_key` varchar(256) NULL,
  `case_status` varchar(32) NOT NULL,
  `severity` varchar(32) NOT NULL,
  `sla_level` varchar(32) NOT NULL,
  `failure_code` varchar(128) NULL,
  `failure_reason` varchar(1000) NULL,
  `idempotency_key` varchar(128) NULL,
  `retryable_flag` tinyint(1) NOT NULL DEFAULT 0,
  `compensable_flag` tinyint(1) NOT NULL DEFAULT 0,
  `high_risk_flag` tinyint(1) NOT NULL DEFAULT 0,
  `legal_hold_flag` tinyint(1) NOT NULL DEFAULT 0,
  `business_evidence_ref_count` int NOT NULL DEFAULT 0,
  `migration_source_ref_count` int NOT NULL DEFAULT 0,
  `before_snapshot_json` longtext NULL,
  `after_snapshot_json` longtext NULL,
  `opened_at` datetime NOT NULL,
  `last_action_at` datetime NULL,
  `resolved_at` datetime NULL,
  `retention_until` datetime NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`operations_case_id`),
  UNIQUE KEY `uk_bpm_operations_case_source` (`source_type`,`source_id`),
  UNIQUE KEY `uk_bpm_operations_case_code` (`case_code`),
  KEY `idx_bpm_operations_case_search` (`business_key`,`graph_definition_version_id`,`assignee_employee_id`,`case_status`),
  KEY `idx_bpm_operations_case_sla` (`sla_level`,`failure_code`,`event_id`),
  KEY `idx_bpm_operations_case_metric` (`graph_definition_version_id`,`definition_node_id`,`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM M7 运营治理统一异常工单';
CREATE TABLE IF NOT EXISTS `t_bpm_operations_action_log` (
  `operations_action_log_id` bigint NOT NULL AUTO_INCREMENT,
  `operations_case_id` bigint NOT NULL,
  `action_type` varchar(32) NOT NULL,
  `action_status` varchar(32) NOT NULL,
  `idempotency_key` varchar(128) NOT NULL,
  `actor_employee_id` bigint NULL,
  `reason` varchar(512) NOT NULL,
  `before_snapshot_json` longtext NOT NULL,
  `after_snapshot_json` longtext NULL,
  `failure_reason` varchar(1000) NULL,
  `action_at` datetime NOT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`operations_action_log_id`),
  UNIQUE KEY `uk_bpm_operations_action_idempotency` (`idempotency_key`),
  KEY `idx_bpm_operations_action_case` (`operations_case_id`,`action_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM M7 运营治理处置审计';
CREATE TABLE IF NOT EXISTS `t_bpm_operations_retention_policy` (
  `retention_policy_id` bigint NOT NULL AUTO_INCREMENT,
  `policy_key` varchar(128) NOT NULL,
  `definition_id` bigint NULL,
  `business_type` varchar(128) NULL,
  `retention_days` int NOT NULL,
  `archive_after_days` int NOT NULL,
  `legal_hold_flag` tinyint(1) NOT NULL DEFAULT 0,
  `status` varchar(32) NOT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`retention_policy_id`),
  UNIQUE KEY `uk_bpm_operations_retention_policy` (`policy_key`),
  KEY `idx_bpm_operations_retention_match` (`definition_id`,`business_type`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM M7 运营治理保留策略';
INSERT INTO `t_bpm_operations_retention_policy`
  (`policy_key`,`definition_id`,`business_type`,`retention_days`,`archive_after_days`,`legal_hold_flag`,`status`,`create_time`,`update_time`)
VALUES ('DEFAULT',NULL,NULL,365,30,0,'ACTIVE',now(),now())
ON DUPLICATE KEY UPDATE `retention_days`=VALUES(`retention_days`),`archive_after_days`=VALUES(`archive_after_days`),`status`=VALUES(`status`),`update_time`=now();
INSERT INTO `t_menu` (`menu_id`,`menu_name`,`menu_type`,`parent_id`,`sort`,`path`,`component`,`perms_type`,`api_perms`,`web_perms`,`icon`,`frame_flag`,`cache_flag`,`visible_flag`,`disabled_flag`,`deleted_flag`,`create_user_id`,`create_time`,`update_time`) VALUES
(375,'运营治理',2,308,24,'/system/bpm/operations/workbench','/system/bpm/operations/workbench.vue',1,NULL,NULL,'ep:operation',0,0,1,0,0,1,now(),now()),
(376,'查询运营治理',3,375,1,NULL,NULL,1,'bpm:operations:query','bpm:operations:query',NULL,0,0,1,0,0,1,now(),now()),
(377,'处置运营治理',3,375,2,NULL,NULL,1,'bpm:operations:update','bpm:operations:update',NULL,0,0,1,0,0,1,now(),now()),
(378,'导出运营治理',3,375,3,NULL,NULL,1,'bpm:operations:export','bpm:operations:export',NULL,0,0,1,0,0,1,now(),now()),
(379,'归档运营治理',3,375,4,NULL,NULL,1,'bpm:operations:archive','bpm:operations:archive',NULL,0,0,1,0,0,1,now(),now()),
(380,'高风险运营治理',3,375,5,NULL,NULL,1,'bpm:operations:high-risk','bpm:operations:high-risk',NULL,0,0,1,0,0,1,now(),now()),
(381,'全域运营治理',3,375,6,NULL,NULL,1,'bpm:operations:all','bpm:operations:all',NULL,0,0,1,0,0,1,now(),now())
ON DUPLICATE KEY UPDATE `menu_name`=VALUES(`menu_name`),`component`=VALUES(`component`),`api_perms`=VALUES(`api_perms`),`web_perms`=VALUES(`web_perms`),`update_time`=now();
INSERT INTO `t_role_menu` (`role_id`,`menu_id`,`create_time`,`update_time`) SELECT 1,m.menu_id,now(),now() FROM t_menu m WHERE m.menu_id IN (375,376,377,378,379,380,381) AND NOT EXISTS (SELECT 1 FROM t_role_menu rm WHERE rm.role_id=1 AND rm.menu_id=m.menu_id);
