-- BPM M2 身份组织与审批策略：版本目录、发布冻结和运行审批阶段。
-- 所有策略版本不可原地修改；Graph 发布保存完整 canonical payload，不在运行期查询最新策略。

ALTER TABLE `t_bpm_candidate_policy_version`
  ADD COLUMN `schema_version` int NOT NULL DEFAULT 1 COMMENT '策略 schema 版本' AFTER `lifecycle_state`,
  ADD COLUMN `policy_digest` char(64) NULL COMMENT 'canonical 策略 SHA-256' AFTER `policy_json`,
  ADD COLUMN `catalog_revision` bigint NOT NULL DEFAULT 0 COMMENT '发布与退休 CAS 版本' AFTER `policy_digest`,
  ADD COLUMN `created_by_employee_id` bigint NULL COMMENT '创建人员工ID' AFTER `catalog_revision`,
  ADD COLUMN `activated_by_employee_id` bigint NULL COMMENT '启用人员工ID' AFTER `created_by_employee_id`,
  ADD COLUMN `activated_at` datetime NULL COMMENT '启用时间' AFTER `activated_by_employee_id`,
  ADD COLUMN `retired_by_employee_id` bigint NULL COMMENT '退休人员工ID' AFTER `activated_at`,
  ADD COLUMN `retired_at` datetime NULL COMMENT '退休时间' AFTER `retired_by_employee_id`,
  ADD COLUMN `effective_risk` varchar(16) NULL COMMENT '服务端推导风险级别' AFTER `retired_at`,
  ADD COLUMN `high_risk_confirmed_by_employee_id` bigint NULL COMMENT '高风险独立确认人' AFTER `effective_risk`,
  ADD COLUMN `high_risk_confirmation_reason` varchar(512) NULL COMMENT '高风险确认原因' AFTER `high_risk_confirmed_by_employee_id`,
  ADD COLUMN `high_risk_confirmed_at` datetime NULL COMMENT '高风险确认时间' AFTER `high_risk_confirmation_reason`,
  ADD COLUMN `high_risk_confirmed_digest` char(64) NULL COMMENT '高风险确认正文摘要' AFTER `high_risk_confirmed_at`;

CREATE TABLE IF NOT EXISTS `t_bpm_approval_policy_version` (
  `approval_policy_version_id` bigint NOT NULL AUTO_INCREMENT COMMENT '审批策略版本ID',
  `policy_key` varchar(64) NOT NULL COMMENT '审批策略编码',
  `policy_version` int NOT NULL COMMENT '审批策略版本号',
  `lifecycle_state` varchar(16) NOT NULL COMMENT 'DRAFT/ACTIVE/RETIRED',
  `schema_version` int NOT NULL DEFAULT 1 COMMENT '策略 schema 版本',
  `policy_json` longtext NOT NULL COMMENT '冻结完成、拒绝、退回与风险策略',
  `policy_digest` char(64) NULL COMMENT 'canonical 策略 SHA-256',
  `catalog_revision` bigint NOT NULL DEFAULT 0 COMMENT '发布与退休 CAS 版本',
  `created_by_employee_id` bigint NULL COMMENT '创建人员工ID',
  `activated_by_employee_id` bigint NULL COMMENT '启用人员工ID',
  `activated_at` datetime NULL COMMENT '启用时间',
  `retired_by_employee_id` bigint NULL COMMENT '退休人员工ID',
  `retired_at` datetime NULL COMMENT '退休时间',
  `effective_risk` varchar(16) NULL COMMENT '服务端推导风险级别',
  `high_risk_confirmed_by_employee_id` bigint NULL COMMENT '高风险独立确认人',
  `high_risk_confirmation_reason` varchar(512) NULL COMMENT '高风险确认原因',
  `high_risk_confirmed_at` datetime NULL COMMENT '高风险确认时间',
  `high_risk_confirmed_digest` char(64) NULL COMMENT '高风险确认正文摘要',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`approval_policy_version_id`),
  UNIQUE KEY `uk_bpm_approval_policy_version` (`policy_key`, `policy_version`),
  KEY `idx_bpm_approval_policy_lifecycle` (`lifecycle_state`, `policy_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM审批策略不可变版本目录';

CREATE TABLE IF NOT EXISTS `t_bpm_start_visibility_policy_version` (
  `start_visibility_policy_version_id` bigint NOT NULL AUTO_INCREMENT COMMENT '发起可见范围策略版本ID',
  `policy_key` varchar(64) NOT NULL COMMENT '发起可见范围策略编码',
  `policy_version` int NOT NULL COMMENT '策略版本号',
  `lifecycle_state` varchar(16) NOT NULL COMMENT 'DRAFT/ACTIVE/RETIRED',
  `schema_version` int NOT NULL DEFAULT 1 COMMENT '策略 schema 版本',
  `policy_json` longtext NOT NULL COMMENT '冻结发起范围与实例可见范围策略',
  `policy_digest` char(64) NULL COMMENT 'canonical 策略 SHA-256',
  `catalog_revision` bigint NOT NULL DEFAULT 0 COMMENT '发布与退休 CAS 版本',
  `created_by_employee_id` bigint NULL COMMENT '创建人员工ID',
  `activated_by_employee_id` bigint NULL COMMENT '启用人员工ID',
  `activated_at` datetime NULL COMMENT '启用时间',
  `retired_by_employee_id` bigint NULL COMMENT '退休人员工ID',
  `retired_at` datetime NULL COMMENT '退休时间',
  `effective_risk` varchar(16) NULL COMMENT '服务端推导风险级别',
  `high_risk_confirmed_by_employee_id` bigint NULL COMMENT '高风险独立确认人',
  `high_risk_confirmation_reason` varchar(512) NULL COMMENT '高风险确认原因',
  `high_risk_confirmed_at` datetime NULL COMMENT '高风险确认时间',
  `high_risk_confirmed_digest` char(64) NULL COMMENT '高风险确认正文摘要',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`start_visibility_policy_version_id`),
  UNIQUE KEY `uk_bpm_start_visibility_policy_version` (`policy_key`, `policy_version`),
  KEY `idx_bpm_start_visibility_policy_lifecycle` (`lifecycle_state`, `policy_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM发起与可见范围策略不可变版本目录';

-- M2 candidate sources: user groups and employee reporting lines.
CREATE TABLE IF NOT EXISTS `t_user_group` (
  `user_group_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户组ID',
  `group_name` varchar(128) NOT NULL COMMENT '用户组名称',
  `disabled_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否禁用',
  `deleted_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`user_group_id`),
  KEY `idx_user_group_active` (`deleted_flag`, `disabled_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组织用户组';

CREATE TABLE IF NOT EXISTS `t_user_group_employee` (
  `user_group_employee_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户组成员关系ID',
  `user_group_id` bigint NOT NULL COMMENT '用户组ID',
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`user_group_employee_id`),
  UNIQUE KEY `uk_user_group_employee` (`user_group_id`, `employee_id`),
  KEY `idx_user_group_employee_employee` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户组员工关系';

CREATE TABLE IF NOT EXISTS `t_employee_reporting_relation` (
  `employee_id` bigint NOT NULL COMMENT '员工ID',
  `manager_employee_id` bigint NOT NULL COMMENT '直属主管员工ID',
  `disabled_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否禁用',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`employee_id`),
  KEY `idx_employee_reporting_manager` (`manager_employee_id`, `disabled_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工汇报关系';

-- M2 command receipts make task actions replayable within an instance.
CREATE TABLE IF NOT EXISTS `t_bpm_approval_command_receipt` (
  `approval_command_receipt_id` bigint NOT NULL AUTO_INCREMENT COMMENT '审批命令回执ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `request_id` varchar(128) NOT NULL COMMENT '客户端请求ID',
  `command_fingerprint` char(64) NOT NULL COMMENT '命令SHA-256指纹',
  `action_type` varchar(32) NOT NULL COMMENT '命令动作',
  `actor_employee_id` bigint NOT NULL COMMENT '服务端认证操作员工ID',
  `receipt_state` varchar(16) NOT NULL COMMENT 'PROCESSING/COMPLETED',
  `response_ok` tinyint(1) NULL COMMENT '响应是否成功',
  `response_code` int NULL COMMENT '响应编码',
  `response_message` varchar(512) NULL COMMENT '响应消息',
  `completed_at` datetime NULL COMMENT '完成时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`approval_command_receipt_id`),
  UNIQUE KEY `uk_bpm_approval_command_request` (`tenant_id`, `instance_id`, `request_id`),
  KEY `idx_bpm_approval_command_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM审批命令幂等回执';

-- Graph 实例与旧定义实例使用互斥来源；Graph 运行期只引用发布版本，绝不回读可变草稿。
ALTER TABLE `t_bpm_graph_definition_version`
  ADD COLUMN `process_name_snapshot` varchar(128) NULL COMMENT '流程名称快照' AFTER `process_key`,
  ADD COLUMN `category_id_snapshot` bigint NULL COMMENT '分类ID快照' AFTER `process_name_snapshot`,
  ADD COLUMN `category_name_snapshot` varchar(128) NULL COMMENT '分类名称快照' AFTER `category_id_snapshot`;

ALTER TABLE `t_bpm_instance`
  MODIFY COLUMN `definition_id` bigint NULL COMMENT '旧定义ID，Graph实例为空',
  ADD COLUMN `graph_definition_version_id` bigint NULL COMMENT 'Graph定义版本ID' AFTER `definition_id`,
  ADD COLUMN `definition_source` varchar(16) NOT NULL DEFAULT 'LEGACY' COMMENT 'LEGACY/GRAPH' AFTER `graph_definition_version_id`,
  ADD COLUMN `start_visibility_policy_version_id` bigint NULL COMMENT '冻结发起可见策略版本ID' AFTER `category_name_snapshot`,
  ADD COLUMN `start_visibility_policy_digest` char(64) NULL COMMENT '冻结发起可见策略摘要' AFTER `start_visibility_policy_version_id`,
  ADD COLUMN `start_visibility_decision_json` longtext NULL COMMENT '发起范围命中决策快照' AFTER `start_visibility_policy_digest`,
  ADD KEY `idx_bpm_instance_graph_definition` (`graph_definition_version_id`);

ALTER TABLE `t_bpm_task`
  MODIFY COLUMN `definition_id` bigint NULL COMMENT '旧定义ID，Graph任务为空',
  ADD COLUMN `graph_definition_version_id` bigint NULL COMMENT 'Graph定义版本ID' AFTER `definition_id`,
  ADD COLUMN `definition_source` varchar(16) NOT NULL DEFAULT 'LEGACY' COMMENT 'LEGACY/GRAPH' AFTER `graph_definition_version_id`,
  ADD KEY `idx_bpm_task_graph_definition` (`graph_definition_version_id`);

ALTER TABLE `t_bpm_task_action_log`
  MODIFY COLUMN `definition_id` bigint NULL COMMENT '旧定义ID，Graph动作日志为空',
  ADD COLUMN `graph_definition_version_id` bigint NULL COMMENT 'Graph定义版本ID' AFTER `definition_id`,
  ADD COLUMN `definition_source` varchar(16) NOT NULL DEFAULT 'LEGACY' COMMENT 'LEGACY/GRAPH' AFTER `graph_definition_version_id`,
  ADD KEY `idx_bpm_task_action_log_graph_definition` (`graph_definition_version_id`);

CREATE TABLE IF NOT EXISTS `t_bpm_approval_stage` (
  `approval_stage_id` bigint NOT NULL AUTO_INCREMENT COMMENT '审批阶段ID',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `definition_version_id` bigint NOT NULL COMMENT '冻结定义版本ID',
  `authored_node_id` varchar(128) NOT NULL COMMENT 'Graph authored审批节点ID',
  `generation` int NOT NULL COMMENT '同一节点运行代次',
  `stage_invocation_id` varchar(128) NOT NULL COMMENT '阶段控制调用唯一标识',
  `engine_process_instance_id` varchar(128) NOT NULL COMMENT 'Flowable流程实例ID',
  `engine_execution_id` varchar(128) NOT NULL COMMENT 'Flowable receive task执行ID',
  `stage_state` varchar(32) NOT NULL COMMENT 'ACTIVE/APPROVED/REJECTED/RETURNED/CANCELLED/EXCEPTION_PENDING',
  `terminal_reason` varchar(128) NULL COMMENT '阶段终态或关闭原因',
  `engine_effect_state` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CLAIMED/COMPLETED/FAILED',
  `engine_effect_claimed_at` datetime NULL COMMENT '引擎副作用领取时间',
  `engine_effect_completed_at` datetime NULL COMMENT '引擎副作用完成时间',
  `engine_effect_error` varchar(512) NULL COMMENT '引擎副作用失败摘要',
  `completion_mode` varchar(32) NOT NULL COMMENT 'SINGLE/SEQUENTIAL/ALL/ANY/RATIO',
  `ratio_percent` int NOT NULL COMMENT '冻结比例百分比',
  `rejection_rule` varchar(64) NOT NULL COMMENT '冻结拒绝规则',
  `effective_member_count` int NOT NULL COMMENT '冻结有效成员数',
  `required_approval_count` int NOT NULL COMMENT '冻结所需通过数',
  `candidate_policy_version_id` bigint NOT NULL COMMENT '候选策略版本ID',
  `candidate_policy_digest` char(64) NOT NULL COMMENT '候选策略内容摘要',
  `approval_policy_version_id` bigint NOT NULL COMMENT '审批策略版本ID',
  `approval_policy_digest` char(64) NOT NULL COMMENT '审批策略内容摘要',
  `approval_policy_snapshot_json` longtext NOT NULL COMMENT '审批策略冻结内容',
  `candidate_snapshot_json` longtext NOT NULL COMMENT '候选成员冻结内容',
  `candidate_snapshot_digest` char(64) NOT NULL COMMENT '候选成员冻结摘要',
  `diagnostics_json` longtext NOT NULL COMMENT '候选解析诊断摘要',
  `opened_at` datetime NOT NULL COMMENT '阶段打开时间',
  `closed_at` datetime NULL COMMENT '阶段关闭时间',
  `revision` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`approval_stage_id`),
  UNIQUE KEY `uk_bpm_approval_stage_instance_node_generation` (`instance_id`, `authored_node_id`, `generation`),
  UNIQUE KEY `uk_bpm_approval_stage_invocation` (`stage_invocation_id`),
  KEY `idx_bpm_approval_stage_instance_state` (`instance_id`, `stage_state`),
  KEY `idx_bpm_approval_stage_definition_node` (`definition_version_id`, `authored_node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM审批阶段冻结事实';

CREATE TABLE IF NOT EXISTS `t_bpm_approval_stage_member` (
  `approval_stage_member_id` bigint NOT NULL AUTO_INCREMENT COMMENT '审批阶段成员ID',
  `approval_stage_id` bigint NOT NULL COMMENT '审批阶段ID',
  `member_order` int NOT NULL COMMENT '冻结成员序号',
  `source_employee_id` bigint NOT NULL COMMENT '候选来源员工ID',
  `current_employee_id` bigint NOT NULL COMMENT '当前处理人员工ID',
  `member_state` varchar(32) NOT NULL COMMENT 'PLANNED/ACTIVE/APPROVED/REJECTED/RETURNED/TERMINATED/INELIGIBLE/CANCELLED',
  `action_result` varchar(32) NULL COMMENT '成员处理结果',
  `task_id` bigint NULL COMMENT 'M4任务投影ID，未激活可为空',
  `candidate_snapshot_digest` char(64) NOT NULL COMMENT '所属候选快照摘要',
  `member_snapshot_json` longtext NOT NULL COMMENT '成员候选解析快照',
  `activated_at` datetime NULL COMMENT '成员激活时间',
  `completed_at` datetime NULL COMMENT '成员完成时间',
  `cancelled_at` datetime NULL COMMENT '成员取消时间',
  `state_changed_at` datetime NOT NULL COMMENT '成员状态变更时间',
  `change_reason` varchar(512) NULL COMMENT '状态变更原因',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`approval_stage_member_id`),
  UNIQUE KEY `uk_bpm_approval_stage_member_order` (`approval_stage_id`, `member_order`),
  UNIQUE KEY `uk_bpm_approval_stage_member_source` (`approval_stage_id`, `source_employee_id`),
  KEY `idx_bpm_approval_stage_member_current_state` (`current_employee_id`, `member_state`),
  KEY `idx_bpm_approval_stage_member_stage_state` (`approval_stage_id`, `member_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM审批阶段成员冻结事实';

-- Graph 审批阶段成员不是 Flowable user task；保留原有 engine_task_id 唯一约束，
-- 由独立的成员关联约束保证每个冻结成员最多拥有一个平台任务投影。
ALTER TABLE `t_bpm_task`
  MODIFY COLUMN `definition_node_id` bigint NULL COMMENT '定义节点ID，Graph审批阶段可为空',
  MODIFY COLUMN `engine_task_id` varchar(128) NULL COMMENT '引擎任务ID，Graph审批成员任务可为空',
  ADD COLUMN `approval_stage_id` bigint NULL COMMENT 'M2审批阶段ID' AFTER `approval_group_id`,
  ADD COLUMN `approval_stage_member_id` bigint NULL COMMENT 'M2审批阶段成员ID' AFTER `approval_stage_id`,
  ADD UNIQUE KEY `uk_bpm_task_approval_stage_member` (`approval_stage_member_id`),
  ADD KEY `idx_bpm_task_approval_stage_state` (`approval_stage_id`, `task_state`),
  ADD KEY `idx_bpm_task_approval_stage_member` (`approval_stage_member_id`);

-- M2 管理页与运行详情使用的字典，重复执行时不覆盖已人工维护的数据。
INSERT INTO `t_dict` (`dict_name`, `dict_code`, `remark`, `disabled_flag`)
SELECT source.`dict_name`, source.`dict_code`, source.`remark`, 0
FROM (
    SELECT 'BPM策略类型' dict_name, 'BPM_POLICY_TYPE' dict_code, '候选、审批和发起可见范围策略类型' remark
    UNION ALL SELECT 'BPM策略生命周期状态', 'BPM_POLICY_LIFECYCLE_STATE', '策略草稿、启用和退休状态'
    UNION ALL SELECT 'BPM审批完成模式', 'BPM_APPROVAL_COMPLETION_MODE', '单人、顺序、会签、或签和比例审批模式'
    UNION ALL SELECT 'BPM审批阶段状态', 'BPM_APPROVAL_STAGE_STATE', '冻结审批阶段运行状态'
    UNION ALL SELECT 'BPM审批阶段成员状态', 'BPM_APPROVAL_STAGE_MEMBER_STATE', '冻结审批成员运行状态'
    UNION ALL SELECT 'BPM模块发布状态', 'BPM_MODULE_RELEASE_STATUS', '模块验收是否满足发布门禁'
) source
WHERE NOT EXISTS (
    SELECT 1 FROM `t_dict` existing WHERE existing.`dict_code` = source.`dict_code`
);

INSERT INTO `t_dict_data`
(`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`)
SELECT dict.`dict_id`, source.`data_value`, source.`data_label`, source.`enum_key`, source.`sort_order`, 0
FROM (
    SELECT 'BPM_POLICY_TYPE' dict_code, 'CANDIDATE' data_value, '候选策略' data_label, 'CANDIDATE' enum_key, 100 sort_order
    UNION ALL SELECT 'BPM_POLICY_TYPE', 'APPROVAL', '审批策略', 'APPROVAL', 90
    UNION ALL SELECT 'BPM_POLICY_TYPE', 'START_VISIBILITY', '发起可见范围策略', 'START_VISIBILITY', 80
    UNION ALL SELECT 'BPM_POLICY_LIFECYCLE_STATE', 'DRAFT', '草稿', 'DRAFT', 100
    UNION ALL SELECT 'BPM_POLICY_LIFECYCLE_STATE', 'ACTIVE', '已启用', 'ACTIVE', 90
    UNION ALL SELECT 'BPM_POLICY_LIFECYCLE_STATE', 'RETIRED', '已退休', 'RETIRED', 80
    UNION ALL SELECT 'BPM_APPROVAL_COMPLETION_MODE', 'SINGLE', '单人审批', 'SINGLE', 100
    UNION ALL SELECT 'BPM_APPROVAL_COMPLETION_MODE', 'SEQUENTIAL', '顺序审批', 'SEQUENTIAL', 90
    UNION ALL SELECT 'BPM_APPROVAL_COMPLETION_MODE', 'ALL', '全员通过', 'ALL', 80
    UNION ALL SELECT 'BPM_APPROVAL_COMPLETION_MODE', 'ANY', '任一通过', 'ANY', 70
    UNION ALL SELECT 'BPM_APPROVAL_COMPLETION_MODE', 'RATIO', '按比例通过', 'RATIO', 60
    UNION ALL SELECT 'BPM_APPROVAL_STAGE_STATE', 'ACTIVE', '审批中', 'ACTIVE', 100
    UNION ALL SELECT 'BPM_APPROVAL_STAGE_STATE', 'APPROVED', '已通过', 'APPROVED', 90
    UNION ALL SELECT 'BPM_APPROVAL_STAGE_STATE', 'REJECTED', '已拒绝', 'REJECTED', 80
    UNION ALL SELECT 'BPM_APPROVAL_STAGE_STATE', 'RETURNED', '已退回', 'RETURNED', 70
    UNION ALL SELECT 'BPM_APPROVAL_STAGE_STATE', 'CANCELLED', '已取消', 'CANCELLED', 60
    UNION ALL SELECT 'BPM_APPROVAL_STAGE_STATE', 'EXCEPTION_PENDING', '等待人工处置', 'EXCEPTION_PENDING', 50
    UNION ALL SELECT 'BPM_APPROVAL_STAGE_MEMBER_STATE', 'PLANNED', '待激活', 'PLANNED', 100
    UNION ALL SELECT 'BPM_APPROVAL_STAGE_MEMBER_STATE', 'ACTIVE', '待审批', 'ACTIVE', 90
    UNION ALL SELECT 'BPM_APPROVAL_STAGE_MEMBER_STATE', 'APPROVED', '已通过', 'APPROVED', 80
    UNION ALL SELECT 'BPM_APPROVAL_STAGE_MEMBER_STATE', 'REJECTED', '已拒绝', 'REJECTED', 70
    UNION ALL SELECT 'BPM_APPROVAL_STAGE_MEMBER_STATE', 'RETURNED', '已退回', 'RETURNED', 60
    UNION ALL SELECT 'BPM_APPROVAL_STAGE_MEMBER_STATE', 'TERMINATED', '已终止', 'TERMINATED', 50
    UNION ALL SELECT 'BPM_APPROVAL_STAGE_MEMBER_STATE', 'INELIGIBLE', '成员失效', 'INELIGIBLE', 40
    UNION ALL SELECT 'BPM_APPROVAL_STAGE_MEMBER_STATE', 'CANCELLED', '已取消', 'CANCELLED', 30
    UNION ALL SELECT 'BPM_MODULE_RELEASE_STATUS', 'RELEASABLE', '可发布', 'RELEASABLE', 100
    UNION ALL SELECT 'BPM_MODULE_RELEASE_STATUS', 'NOT_RELEASABLE', '不可发布', 'NOT_RELEASABLE', 90
    UNION ALL SELECT 'BPM_TASK_ACTION_LOG_TYPE', 'M2_MEMBER_TRANSFERRED', 'M2成员受控转办', 'M2_MEMBER_TRANSFERRED', 45
    UNION ALL SELECT 'BPM_TASK_ACTION_LOG_TYPE', 'M2_MEMBER_INELIGIBLE', 'M2成员失效', 'M2_MEMBER_INELIGIBLE', 44
) source
JOIN `t_dict` dict ON dict.`dict_code` = source.`dict_code`
LEFT JOIN `t_dict_data` existing
  ON existing.`dict_id` = dict.`dict_id`
 AND existing.`data_value` = source.`data_value`
WHERE existing.`dict_data_id` IS NULL;

-- 策略目录页面、接口权限和超级管理员初始授权。
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
VALUES
  (342, '审批策略目录', 2, 308, 15, '/system/bpm/policy/policy-catalog', '/system/bpm/policy/policy-catalog.vue', 1, NULL, NULL, 'ep:document-checked', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (343, '查询审批策略目录', 3, 342, 1, NULL, NULL, 1, 'bpm:policy-catalog:list', 'bpm:policy-catalog:list', NULL, 342, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (344, '查看审批策略版本', 3, 342, 2, NULL, NULL, 1, 'bpm:policy-catalog:detail', 'bpm:policy-catalog:detail', NULL, 342, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (345, '新建审批策略草稿', 3, 342, 3, NULL, NULL, 1, 'bpm:policy-catalog:add', 'bpm:policy-catalog:add', NULL, 342, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (346, '复制审批策略版本', 3, 342, 4, NULL, NULL, 1, 'bpm:policy-catalog:copy', 'bpm:policy-catalog:copy', NULL, 342, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (347, '启用审批策略版本', 3, 342, 5, NULL, NULL, 1, 'bpm:policy-catalog:activate', 'bpm:policy-catalog:activate', NULL, 342, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (348, '退休审批策略版本', 3, 342, 6, NULL, NULL, 1, 'bpm:policy-catalog:retire', 'bpm:policy-catalog:retire', NULL, 342, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (349, '独立确认高风险策略', 3, 342, 7, NULL, NULL, 1, 'bpm:policy-catalog:activate-high-risk', 'bpm:policy-catalog:activate-high-risk', NULL, 342, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now())
  ,(350, '受控转办M2审批成员', 3, 314, 1, NULL, NULL, 1, 'bpm:task:m2-member-transfer', 'bpm:task:m2-member-transfer', NULL, 314, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_type` = VALUES(`menu_type`),
  `parent_id` = VALUES(`parent_id`), `sort` = VALUES(`sort`),
  `path` = VALUES(`path`), `component` = VALUES(`component`),
  `perms_type` = VALUES(`perms_type`), `api_perms` = VALUES(`api_perms`),
  `web_perms` = VALUES(`web_perms`), `icon` = VALUES(`icon`),
  `context_menu_id` = VALUES(`context_menu_id`), `visible_flag` = VALUES(`visible_flag`),
  `disabled_flag` = VALUES(`disabled_flag`), `deleted_flag` = VALUES(`deleted_flag`),
  `update_user_id` = VALUES(`update_user_id`), `update_time` = now();

INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, menu.`menu_id`, now(), now()
FROM `t_menu` menu
WHERE menu.`menu_id` IN (342, 343, 344, 345, 346, 347, 348, 349, 350)
  AND menu.`deleted_flag` = 0
  AND menu.`disabled_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `t_role_menu` role_menu
    WHERE role_menu.`role_id` = 1 AND role_menu.`menu_id` = menu.`menu_id`
  );
