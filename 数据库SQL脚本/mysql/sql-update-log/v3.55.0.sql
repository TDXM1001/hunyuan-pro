-- BPM M3：审批对象与数据治理四类数据面。
-- 执行前提：已执行 v3.52.0（业务契约目录）和 v3.54.0（Graph 运行投影）。
-- 兼容影响：仅新增表、实例引用列和索引；旧实例引用列保持 NULL，原运行数据不被改写。
-- 恢复方式：仅当新表尚未写入审批数据且实例新列均为 NULL 时，方可删除新增列并 DROP 下列表。

SET @m3_contract_columns_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 't_bpm_business_contract_version'
     AND column_name IN (
       'schema_version', 'contract_digest', 'catalog_revision', 'created_by_employee_id',
       'activated_by_employee_id', 'activated_at', 'retired_by_employee_id', 'retired_at'
     )) = 8,
  'SELECT 1',
  'ALTER TABLE `t_bpm_business_contract_version`
     ADD COLUMN `schema_version` int NOT NULL DEFAULT 1 COMMENT ''契约Schema版本'' AFTER `lifecycle_state`,
     ADD COLUMN `contract_digest` char(64) NULL COMMENT ''规范化契约SHA-256'' AFTER `contract_json`,
     ADD COLUMN `catalog_revision` bigint NOT NULL DEFAULT 0 COMMENT ''目录CAS修订号'' AFTER `contract_digest`,
     ADD COLUMN `created_by_employee_id` bigint NULL COMMENT ''创建人员工ID'' AFTER `catalog_revision`,
     ADD COLUMN `activated_by_employee_id` bigint NULL COMMENT ''启用人员工ID'' AFTER `created_by_employee_id`,
     ADD COLUMN `activated_at` datetime NULL COMMENT ''启用时间'' AFTER `activated_by_employee_id`,
     ADD COLUMN `retired_by_employee_id` bigint NULL COMMENT ''退休人员工ID'' AFTER `activated_at`,
     ADD COLUMN `retired_at` datetime NULL COMMENT ''退休时间'' AFTER `retired_by_employee_id`'
);
PREPARE m3_contract_columns_stmt FROM @m3_contract_columns_sql;
EXECUTE m3_contract_columns_stmt;
DEALLOCATE PREPARE m3_contract_columns_stmt;

CREATE TABLE IF NOT EXISTS `t_bpm_approval_subject_snapshot` (
  `approval_subject_snapshot_id` bigint NOT NULL AUTO_INCREMENT COMMENT '审批对象快照ID',
  `business_contract_version_id` bigint NOT NULL COMMENT '冻结业务契约版本ID',
  `source_system` varchar(64) NOT NULL COMMENT '来源系统编码',
  `business_type` varchar(128) NOT NULL COMMENT '业务类型',
  `business_key` varchar(256) NOT NULL COMMENT '字符串业务键',
  `subject_version` bigint NOT NULL COMMENT '审批对象版本',
  `title` varchar(256) NOT NULL COMMENT '审批标题',
  `summary` varchar(1000) NULL COMMENT '审批摘要',
  `fields_json` longtext NOT NULL COMMENT '只读审批字段快照',
  `line_items_json` longtext NOT NULL COMMENT '审批明细快照',
  `attachments_json` longtext NOT NULL COMMENT '审批附件快照',
  `submitter_employee_id` bigint NOT NULL COMMENT '提交人员工ID',
  `submitter_name_snapshot` varchar(128) NOT NULL COMMENT '提交人姓名快照',
  `snapshot_state` varchar(32) NOT NULL COMMENT 'ACTIVE/FINAL',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`approval_subject_snapshot_id`),
  UNIQUE KEY `uk_bpm_approval_subject_business_identity` (`source_system`, `business_type`, `business_key`, `subject_version`),
  KEY `idx_bpm_approval_subject_contract` (`business_contract_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM审批对象不可变快照';

CREATE TABLE IF NOT EXISTS `t_bpm_routing_fact_snapshot` (
  `routing_fact_snapshot_id` bigint NOT NULL AUTO_INCREMENT COMMENT '路由事实快照ID',
  `approval_subject_snapshot_id` bigint NOT NULL COMMENT '审批对象快照ID',
  `business_contract_version_id` bigint NOT NULL COMMENT '冻结业务契约版本ID',
  `routing_fact_version` bigint NOT NULL COMMENT '路由事实版本',
  `facts_json` longtext NOT NULL COMMENT '已校验脱敏路由事实',
  `allowed_fact_keys_json` longtext NOT NULL COMMENT '允许路由与候选使用的事实键',
  `snapshot_digest` char(64) NOT NULL COMMENT '事实摘要SHA-256',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`routing_fact_snapshot_id`),
  UNIQUE KEY `uk_bpm_routing_fact_subject_version` (`approval_subject_snapshot_id`, `routing_fact_version`),
  KEY `idx_bpm_routing_fact_contract` (`business_contract_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM冻结路由事实';

CREATE TABLE IF NOT EXISTS `t_bpm_process_working_data` (
  `process_working_data_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流程工作数据ID',
  `approval_subject_snapshot_id` bigint NOT NULL COMMENT '审批对象快照ID',
  `data_version` bigint NOT NULL COMMENT '工作数据版本',
  `data_json` longtext NOT NULL COMMENT '完整工作数据快照',
  `actor_employee_id` bigint NOT NULL COMMENT '变更人员工ID',
  `actor_name_snapshot` varchar(128) NOT NULL COMMENT '变更人姓名快照',
  `change_reason` varchar(512) NOT NULL COMMENT '变更原因',
  `previous_data_version` bigint NULL COMMENT '前一工作数据版本',
  `data_digest` char(64) NOT NULL COMMENT '数据摘要SHA-256',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`process_working_data_id`),
  UNIQUE KEY `uk_bpm_working_data_subject_version` (`approval_subject_snapshot_id`, `data_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM版本化流程工作数据';

CREATE TABLE IF NOT EXISTS `t_bpm_task_action_evidence` (
  `task_action_evidence_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务动作证据ID',
  `approval_subject_snapshot_id` bigint NOT NULL COMMENT '审批对象快照ID',
  `task_id` bigint NULL COMMENT '平台任务ID',
  `action_type` varchar(64) NOT NULL COMMENT '动作类型',
  `actor_employee_id` bigint NOT NULL COMMENT '服务端认证操作员工ID',
  `actor_name_snapshot` varchar(128) NOT NULL COMMENT '操作人姓名快照',
  `action_reason` varchar(512) NOT NULL COMMENT '动作原因',
  `comment_text` varchar(2000) NULL COMMENT '审批意见',
  `signature_json` longtext NULL COMMENT '签名证据',
  `attachments_json` longtext NOT NULL COMMENT '动作附件',
  `before_working_data_version` bigint NOT NULL COMMENT '动作前工作数据版本',
  `after_working_data_version` bigint NOT NULL COMMENT '动作后工作数据版本',
  `changed_fields_json` longtext NOT NULL COMMENT '变更字段集合',
  `before_data_json` longtext NOT NULL COMMENT '动作前工作数据快照',
  `after_data_json` longtext NOT NULL COMMENT '动作后工作数据快照',
  `evidence_digest` char(64) NOT NULL COMMENT '证据摘要SHA-256',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`task_action_evidence_id`),
  KEY `idx_bpm_action_evidence_subject` (`approval_subject_snapshot_id`, `create_time`),
  KEY `idx_bpm_action_evidence_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM任务动作不可变证据';

SET @m3_instance_columns_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 't_bpm_instance'
     AND column_name IN (
       'approval_subject_snapshot_id', 'routing_fact_snapshot_id', 'process_working_data_id'
     )) = 3,
  'SELECT 1',
  'ALTER TABLE `t_bpm_instance`
     ADD COLUMN `approval_subject_snapshot_id` bigint NULL COMMENT ''冻结审批对象快照ID'' AFTER `business_key`,
     ADD COLUMN `routing_fact_snapshot_id` bigint NULL COMMENT ''冻结路由事实快照ID'' AFTER `approval_subject_snapshot_id`,
     ADD COLUMN `process_working_data_id` bigint NULL COMMENT ''当前流程工作数据ID'' AFTER `routing_fact_snapshot_id`,
     ADD UNIQUE KEY `uk_bpm_instance_approval_subject` (`approval_subject_snapshot_id`)'
);
PREPARE m3_instance_columns_stmt FROM @m3_instance_columns_sql;
EXECUTE m3_instance_columns_stmt;
DEALLOCATE PREPARE m3_instance_columns_stmt;

-- M3 业务契约、数据敏感级别和变化策略字典。
INSERT INTO `t_dict` (`dict_name`, `dict_code`, `remark`, `disabled_flag`)
SELECT source.`dict_name`, source.`dict_code`, source.`remark`, 0
FROM (
    SELECT 'BPM业务契约生命周期' dict_name, 'BPM_BUSINESS_CONTRACT_LIFECYCLE' dict_code, '业务契约草稿、启用和退休状态' remark
    UNION ALL SELECT 'BPM数据敏感级别', 'BPM_DATA_SENSITIVITY', '审批对象字段服务端授权级别'
    UNION ALL SELECT 'BPM数据变化策略', 'BPM_DATA_CHANGE_POLICY', '业务对象审批期间变化语义'
) source
WHERE NOT EXISTS (
    SELECT 1 FROM `t_dict` existing WHERE existing.`dict_code` = source.`dict_code`
);

INSERT INTO `t_dict_data`
(`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`)
SELECT dict.`dict_id`, source.`data_value`, source.`data_label`, source.`data_value`, source.`sort_order`, 0
FROM (
    SELECT 'BPM_BUSINESS_CONTRACT_LIFECYCLE' dict_code, 'DRAFT' data_value, '草稿' data_label, 100 sort_order
    UNION ALL SELECT 'BPM_BUSINESS_CONTRACT_LIFECYCLE', 'ACTIVE', '已启用', 90
    UNION ALL SELECT 'BPM_BUSINESS_CONTRACT_LIFECYCLE', 'RETIRED', '已退休', 80
    UNION ALL SELECT 'BPM_DATA_SENSITIVITY', 'PUBLIC', '公开', 100
    UNION ALL SELECT 'BPM_DATA_SENSITIVITY', 'INTERNAL', '内部', 90
    UNION ALL SELECT 'BPM_DATA_SENSITIVITY', 'CONFIDENTIAL', '机密', 80
    UNION ALL SELECT 'BPM_DATA_SENSITIVITY', 'RESTRICTED', '严格受限', 70
    UNION ALL SELECT 'BPM_DATA_CHANGE_POLICY', 'LOCKED', '审批中锁定', 100
    UNION ALL SELECT 'BPM_DATA_CHANGE_POLICY', 'VERSIONED', '允许版本化变更', 90
    UNION ALL SELECT 'BPM_DATA_CHANGE_POLICY', 'RESTART_REQUIRED', '变化后重新审批', 80
    UNION ALL SELECT 'BPM_DATA_CHANGE_POLICY', 'FIELD_CONTROLLED', '字段白名单控制', 70
) source
JOIN `t_dict` dict ON dict.`dict_code` = source.`dict_code`
LEFT JOIN `t_dict_data` existing
  ON existing.`dict_id` = dict.`dict_id` AND existing.`data_value` = source.`data_value`
WHERE existing.`dict_data_id` IS NULL;

-- 契约目录页面、通用申请页面及一致的接口/按钮权限。
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
VALUES
  (351, '业务契约目录', 2, 308, 16, '/system/bpm/business-contract/business-contract-catalog', '/system/bpm/business-contract/business-contract-catalog.vue', 1, NULL, NULL, 'ep:document', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (352, '查询业务契约目录', 3, 351, 1, NULL, NULL, 1, 'bpm:business-contract:list', 'bpm:business-contract:list', NULL, 351, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (353, '查看业务契约版本', 3, 351, 2, NULL, NULL, 1, 'bpm:business-contract:detail', 'bpm:business-contract:detail', NULL, 351, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (354, '新建业务契约草稿', 3, 351, 3, NULL, NULL, 1, 'bpm:business-contract:add', 'bpm:business-contract:add', NULL, 351, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (355, '复制业务契约版本', 3, 351, 4, NULL, NULL, 1, 'bpm:business-contract:copy', 'bpm:business-contract:copy', NULL, 351, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (356, '启用业务契约版本', 3, 351, 5, NULL, NULL, 1, 'bpm:business-contract:activate', 'bpm:business-contract:activate', NULL, 351, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (357, '退休业务契约版本', 3, 351, 6, NULL, NULL, 1, 'bpm:business-contract:retire', 'bpm:business-contract:retire', NULL, 351, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (358, '通用申请', 2, 308, 17, '/system/bpm/runtime/generic-application', '/system/bpm/runtime/generic-application.vue', 1, NULL, NULL, 'ep:edit-pen', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_type` = VALUES(`menu_type`),
  `parent_id` = VALUES(`parent_id`), `sort` = VALUES(`sort`),
  `path` = VALUES(`path`), `component` = VALUES(`component`),
  `perms_type` = VALUES(`perms_type`), `api_perms` = VALUES(`api_perms`),
  `web_perms` = VALUES(`web_perms`), `icon` = VALUES(`icon`),
  `context_menu_id` = VALUES(`context_menu_id`), `visible_flag` = VALUES(`visible_flag`),
  `disabled_flag` = VALUES(`disabled_flag`), `deleted_flag` = VALUES(`deleted_flag`),
  `update_time` = now();

INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, menu.`menu_id`, now(), now()
FROM `t_menu` menu
WHERE menu.`menu_id` IN (351, 352, 353, 354, 355, 356, 357, 358)
  AND menu.`deleted_flag` = 0 AND menu.`disabled_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `t_role_menu` role_menu
    WHERE role_menu.`role_id` = 1 AND role_menu.`menu_id` = menu.`menu_id`
  );
