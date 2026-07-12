-- BPM M4：时间、SLA、登记连接器与外部等待运行事实。
CREATE TABLE IF NOT EXISTS `t_bpm_time_event` (
  `time_event_id` bigint NOT NULL AUTO_INCREMENT COMMENT '时间事件ID',
  `event_key` varchar(160) NOT NULL COMMENT '事件唯一键',
  `idempotency_key` varchar(160) NOT NULL COMMENT '业务幂等键',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `task_id` bigint NULL COMMENT '任务ID',
  `definition_id` bigint NOT NULL COMMENT '定义ID',
  `definition_node_id` bigint NULL COMMENT '定义节点ID',
  `node_key` varchar(128) NOT NULL COMMENT '设计节点编码',
  `engine_process_instance_id` varchar(128) NOT NULL COMMENT '引擎实例ID',
  `engine_execution_id` varchar(128) NULL COMMENT '引擎执行ID',
  `engine_task_id` varchar(128) NULL COMMENT '引擎任务ID',
  `engine_job_id` varchar(128) NULL COMMENT 'Flowable计时任务ID',
  `event_kind` varchar(32) NOT NULL COMMENT '事件类型',
  `policy_snapshot_json` longtext NOT NULL COMMENT '冻结策略快照',
  `scheduled_at` datetime NOT NULL COMMENT '计划触发时间',
  `triggered_at` datetime NULL COMMENT '实际触发时间',
  `completed_at` datetime NULL COMMENT '完成时间',
  `event_status` varchar(32) NOT NULL COMMENT '事件状态',
  `trigger_count` int NOT NULL DEFAULT 0 COMMENT '触发次数',
  `last_error` varchar(2000) NULL COMMENT '最后错误',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`time_event_id`),
  UNIQUE KEY `uk_bpm_time_event_key` (`event_key`),
  UNIQUE KEY `uk_bpm_time_event_idempotency` (`idempotency_key`),
  KEY `idx_bpm_time_event_schedule` (`event_status`, `scheduled_at`),
  KEY `idx_bpm_time_event_instance` (`instance_id`, `create_time`),
  KEY `idx_bpm_time_event_task` (`task_id`, `event_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM时间事件与SLA事实';

CREATE TABLE IF NOT EXISTS `t_bpm_external_wait` (
  `external_wait_id` bigint NOT NULL AUTO_INCREMENT COMMENT '外部等待ID',
  `correlation_key` varchar(160) NOT NULL COMMENT '内部相关键',
  `callback_token_hash` char(64) NOT NULL COMMENT '回调令牌SHA-256摘要',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `definition_id` bigint NOT NULL COMMENT '定义ID',
  `definition_node_id` bigint NOT NULL COMMENT '定义节点ID',
  `engine_process_instance_id` varchar(128) NOT NULL COMMENT '引擎实例ID',
  `engine_execution_id` varchar(128) NOT NULL COMMENT '等待执行ID',
  `node_key` varchar(128) NOT NULL COMMENT '设计节点编码',
  `connector_key` varchar(64) NOT NULL COMMENT '连接器编码',
  `connector_version` int NOT NULL COMMENT '冻结连接器版本',
  `operation_key` varchar(64) NOT NULL COMMENT '操作编码',
  `attempt_no` int NOT NULL DEFAULT 1 COMMENT '执行代次',
  `request_snapshot_json` longtext NULL COMMENT '脱敏请求快照',
  `callback_payload_snapshot_json` longtext NULL COMMENT '脱敏回调快照',
  `wait_status` varchar(32) NOT NULL COMMENT '等待状态',
  `timeout_at` datetime NOT NULL COMMENT '等待超时时间',
  `resumed_at` datetime NULL COMMENT '恢复时间',
  `cancelled_at` datetime NULL COMMENT '取消时间',
  `last_error` varchar(2000) NULL COMMENT '最后错误',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`external_wait_id`),
  UNIQUE KEY `uk_bpm_external_wait_correlation` (`correlation_key`),
  UNIQUE KEY `uk_bpm_external_wait_token` (`callback_token_hash`),
  KEY `idx_bpm_external_wait_status` (`wait_status`, `timeout_at`),
  KEY `idx_bpm_external_wait_instance` (`instance_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM外部回调等待事实';

CREATE TABLE IF NOT EXISTS `t_bpm_connector_definition` (
  `connector_definition_id` bigint NOT NULL AUTO_INCREMENT COMMENT '连接器定义ID',
  `connector_key` varchar(64) NOT NULL COMMENT '连接器编码',
  `connector_version` int NOT NULL COMMENT '连接器版本',
  `connector_name` varchar(128) NOT NULL COMMENT '连接器名称',
  `base_endpoint_ref` varchar(256) NOT NULL COMMENT '登记端点安全引用',
  `credential_ref` varchar(256) NULL COMMENT '凭据安全引用',
  `allowed_operations_json` longtext NOT NULL COMMENT '允许操作与幂等属性',
  `timeout_millis` int NOT NULL DEFAULT 5000 COMMENT '请求超时毫秒',
  `retry_policy_json` longtext NOT NULL COMMENT '重试策略',
  `circuit_policy_json` longtext NULL COMMENT '熔断策略',
  `request_schema_json` longtext NOT NULL COMMENT '请求白名单schema',
  `response_schema_json` longtext NOT NULL COMMENT '响应白名单schema',
  `enabled_state` varchar(32) NOT NULL COMMENT '启用状态',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`connector_definition_id`),
  UNIQUE KEY `uk_bpm_connector_version` (`connector_key`, `connector_version`),
  KEY `idx_bpm_connector_state` (`enabled_state`, `connector_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM登记连接器定义';

-- 字典类型使用 NOT EXISTS，允许已人工补录部分 M4 字典的环境重复执行。
INSERT INTO `t_dict` (`dict_name`, `dict_code`, `remark`, `disabled_flag`)
SELECT source.`dict_name`, source.`dict_code`, source.`remark`, 0
FROM (
    SELECT 'BPM时间事件状态' dict_name, 'BPM_TIME_EVENT_STATUS' dict_code, 'SLA和延迟时间事件状态' remark
    UNION ALL SELECT 'BPM时间事件类型', 'BPM_TIME_EVENT_KIND', '时间事件业务类型'
    UNION ALL SELECT 'BPM外部等待状态', 'BPM_EXTERNAL_WAIT_STATUS', '回调等待运行状态'
    UNION ALL SELECT 'BPM超时动作', 'BPM_TIMEOUT_ACTION', '任务SLA超时动作'
    UNION ALL SELECT 'BPM延迟模式', 'BPM_DELAY_MODE', '受控延迟时间来源'
    UNION ALL SELECT 'BPM外部等待模式', 'BPM_EXTERNAL_WAIT_MODE', '连接器调用等待模式'
    UNION ALL SELECT 'BPM连接器状态', 'BPM_CONNECTOR_STATE', '登记连接器启停状态'
    UNION ALL SELECT 'BPM流程风险等级', 'BPM_PROCESS_RISK_LEVEL', '自动终态风险控制等级'
) source
WHERE NOT EXISTS (
    SELECT 1 FROM `t_dict` existing WHERE existing.`dict_code` = source.`dict_code`
);

INSERT INTO `t_dict_data`
(`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`)
SELECT dict.`dict_id`, source.`data_value`, source.`data_label`, source.`enum_key`, source.`sort_order`, 0
FROM (
    SELECT 'BPM_TIME_EVENT_STATUS' dict_code, 'SCHEDULED' data_value, '已计划' data_label, 'SCHEDULED' enum_key, 100 sort_order
    UNION ALL SELECT 'BPM_TIME_EVENT_STATUS', 'TRIGGERED', '已触发', 'TRIGGERED', 90
    UNION ALL SELECT 'BPM_TIME_EVENT_STATUS', 'SUCCEEDED', '执行成功', 'SUCCEEDED', 80
    UNION ALL SELECT 'BPM_TIME_EVENT_STATUS', 'FAILED_RETRYABLE', '失败可重试', 'FAILED_RETRYABLE', 70
    UNION ALL SELECT 'BPM_TIME_EVENT_STATUS', 'FAILED_MANUAL', '等待人工处理', 'FAILED_MANUAL', 60
    UNION ALL SELECT 'BPM_TIME_EVENT_STATUS', 'CANCELLED', '已取消', 'CANCELLED', 50
    UNION ALL SELECT 'BPM_TIME_EVENT_KIND', 'SLA_REMINDER', 'SLA提醒', 'SLA_REMINDER', 100
    UNION ALL SELECT 'BPM_TIME_EVENT_KIND', 'SLA_DUE', 'SLA到期', 'SLA_DUE', 90
    UNION ALL SELECT 'BPM_TIME_EVENT_KIND', 'DELAY', '延迟节点', 'DELAY', 80
    UNION ALL SELECT 'BPM_TIME_EVENT_KIND', 'EXTERNAL_TIMEOUT', '外部等待超时', 'EXTERNAL_TIMEOUT', 70
    UNION ALL SELECT 'BPM_EXTERNAL_WAIT_STATUS', 'WAITING', '等待回调', 'WAITING', 100
    UNION ALL SELECT 'BPM_EXTERNAL_WAIT_STATUS', 'RESUMED', '已恢复', 'RESUMED', 90
    UNION ALL SELECT 'BPM_EXTERNAL_WAIT_STATUS', 'TIMED_OUT', '已超时', 'TIMED_OUT', 80
    UNION ALL SELECT 'BPM_EXTERNAL_WAIT_STATUS', 'CANCELLED', '已取消', 'CANCELLED', 70
    UNION ALL SELECT 'BPM_EXTERNAL_WAIT_STATUS', 'FAILED_MANUAL', '等待人工处理', 'FAILED_MANUAL', 60
    UNION ALL SELECT 'BPM_TIMEOUT_ACTION', 'NONE', '无动作', 'NONE', 100
    UNION ALL SELECT 'BPM_TIMEOUT_ACTION', 'REMIND_ONLY', '仅提醒', 'REMIND_ONLY', 90
    UNION ALL SELECT 'BPM_TIMEOUT_ACTION', 'AUTO_APPROVE', '自动通过', 'AUTO_APPROVE', 80
    UNION ALL SELECT 'BPM_TIMEOUT_ACTION', 'AUTO_REJECT', '自动拒绝', 'AUTO_REJECT', 70
    UNION ALL SELECT 'BPM_TIMEOUT_ACTION', 'ASSIGN_ADMIN', '转管理员', 'ASSIGN_ADMIN', 60
    UNION ALL SELECT 'BPM_DELAY_MODE', 'DURATION', '固定时长', 'DURATION', 100
    UNION ALL SELECT 'BPM_DELAY_MODE', 'FIXED_DATETIME', '固定日期时间', 'FIXED_DATETIME', 90
    UNION ALL SELECT 'BPM_DELAY_MODE', 'FORM_DATETIME', '表单日期时间', 'FORM_DATETIME', 80
    UNION ALL SELECT 'BPM_EXTERNAL_WAIT_MODE', 'NO_WAIT', '调用后继续', 'NO_WAIT', 100
    UNION ALL SELECT 'BPM_EXTERNAL_WAIT_MODE', 'WAIT_CALLBACK', '等待回调', 'WAIT_CALLBACK', 90
    UNION ALL SELECT 'BPM_CONNECTOR_STATE', 'ENABLED', '已启用', 'ENABLED', 100
    UNION ALL SELECT 'BPM_CONNECTOR_STATE', 'DISABLED', '已停用', 'DISABLED', 90
    UNION ALL SELECT 'BPM_CONNECTOR_STATE', 'EMERGENCY_DISABLED', '紧急停用', 'EMERGENCY_DISABLED', 80
    UNION ALL SELECT 'BPM_PROCESS_RISK_LEVEL', 'LOW', '低风险', 'LOW', 100
    UNION ALL SELECT 'BPM_PROCESS_RISK_LEVEL', 'MEDIUM', '中风险', 'MEDIUM', 90
    UNION ALL SELECT 'BPM_PROCESS_RISK_LEVEL', 'HIGH', '高风险', 'HIGH', 80
    UNION ALL SELECT 'BPM_PROCESS_NODE_TYPE', 'DELAY', '延迟节点', 'DELAY', 40
    UNION ALL SELECT 'BPM_PROCESS_NODE_TYPE', 'EXTERNAL_TRIGGER', '外部触发节点', 'EXTERNAL_TRIGGER', 30
    UNION ALL SELECT 'BPM_TASK_ACTION_LOG_TYPE', 'SYSTEM_AUTO_APPROVED', '系统超时自动通过', 'SYSTEM_AUTO_APPROVED', 5
    UNION ALL SELECT 'BPM_TASK_ACTION_LOG_TYPE', 'SYSTEM_AUTO_REJECTED', '系统超时自动拒绝', 'SYSTEM_AUTO_REJECTED', 4
    UNION ALL SELECT 'BPM_TASK_ACTION_LOG_TYPE', 'SLA_REMINDER_SENT', 'SLA提醒已发送', 'SLA_REMINDER_SENT', 3
    UNION ALL SELECT 'BPM_TASK_ACTION_LOG_TYPE', 'EXTERNAL_WAIT_RESUMED', '外部等待已恢复', 'EXTERNAL_WAIT_RESUMED', 2
    UNION ALL SELECT 'BPM_TASK_ACTION_LOG_TYPE', 'SYSTEM_SLA_ASSIGNED_ADMIN', 'SLA超时转管理员', 'SYSTEM_SLA_ASSIGNED_ADMIN', 1
) source
JOIN `t_dict` dict ON dict.`dict_code` = source.`dict_code`
LEFT JOIN `t_dict_data` existing
  ON existing.`dict_id` = dict.`dict_id`
 AND existing.`data_value` = source.`data_value`
WHERE existing.`dict_data_id` IS NULL;

-- M4 运营页面、连接器目录和接口权限。
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
VALUES
  (326, '时间事件运营', 1, 308, 14, '/system/bpm/time-event', NULL, 1, NULL, NULL, 'ep:timer', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (327, '时间事件', 2, 326, 1, '/system/bpm/time-event/time-event-list', '/system/bpm/time-event/time-event-list.vue', 1, NULL, NULL, 'ep:alarm-clock', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (328, '外部等待', 2, 326, 2, '/system/bpm/time-event/external-wait-list', '/system/bpm/time-event/external-wait-list.vue', 1, NULL, NULL, 'ep:connection', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (329, '查询时间事件', 3, 326, 1, NULL, NULL, 1, 'bpm:time-event:query', 'bpm:time-event:query', NULL, 326, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (330, '处置时间事件', 3, 326, 2, NULL, NULL, 1, 'bpm:time-event:update', 'bpm:time-event:update', NULL, 326, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (331, '连接器目录', 2, 321, 3, '/system/bpm/integration/connector-list', '/system/bpm/integration/connector-list.vue', 1, NULL, NULL, 'ep:link', NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (332, '查询连接器', 3, 321, 3, NULL, NULL, 1, 'bpm:connector:query', 'bpm:connector:query', NULL, 321, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (333, '维护连接器', 3, 321, 4, NULL, NULL, 1, 'bpm:connector:update', 'bpm:connector:update', NULL, 321, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_type` = VALUES(`menu_type`),
  `parent_id` = VALUES(`parent_id`), `sort` = VALUES(`sort`),
  `path` = VALUES(`path`), `component` = VALUES(`component`),
  `api_perms` = VALUES(`api_perms`), `web_perms` = VALUES(`web_perms`),
  `icon` = VALUES(`icon`), `context_menu_id` = VALUES(`context_menu_id`),
  `visible_flag` = VALUES(`visible_flag`), `disabled_flag` = VALUES(`disabled_flag`),
  `deleted_flag` = VALUES(`deleted_flag`), `update_time` = now();

INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, menu.`menu_id`, now(), now()
FROM `t_menu` menu
WHERE menu.`menu_id` IN (326, 327, 328, 329, 330, 331, 332, 333)
  AND menu.`deleted_flag` = 0
  AND menu.`disabled_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `t_role_menu` role_menu
    WHERE role_menu.`role_id` = 1 AND role_menu.`menu_id` = menu.`menu_id`
  );
