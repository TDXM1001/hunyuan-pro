-- BPM P1.1：流程定义可发起范围治理
ALTER TABLE `t_bpm_definition`
    ADD COLUMN `start_scope_json` longtext NULL COMMENT '可发起范围快照JSON' AFTER `start_state`;

CREATE INDEX `idx_definition_start_state` ON `t_bpm_definition` (`start_state`);

-- BPM P1.3：业务接入可靠性记录
CREATE TABLE `t_bpm_callback_record` (
  `callback_record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '回调记录ID',
  `event_id` varchar(128) NOT NULL COMMENT '事件ID',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `business_type` varchar(64) NOT NULL COMMENT '业务类型',
  `business_id` bigint NOT NULL COMMENT '业务ID',
  `callback_status` tinyint NOT NULL COMMENT '回调状态',
  `request_payload_json` longtext NULL COMMENT '请求载荷',
  `response_payload_json` longtext NULL COMMENT '响应载荷',
  `failure_reason` varchar(1000) NULL COMMENT '失败原因',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `next_retry_at` datetime NULL COMMENT '下次重试时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`callback_record_id`),
  UNIQUE KEY `uk_bpm_callback_event` (`event_id`),
  KEY `idx_bpm_callback_status` (`callback_status`, `next_retry_at`),
  KEY `idx_bpm_callback_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM业务回调记录';

CREATE TABLE `t_bpm_command_record` (
  `command_record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '命令记录ID',
  `command_key` varchar(128) NOT NULL COMMENT '命令幂等键',
  `command_type` varchar(64) NOT NULL COMMENT '命令类型',
  `instance_id` bigint NULL COMMENT '流程实例ID',
  `business_type` varchar(64) NULL COMMENT '业务类型',
  `business_id` bigint NULL COMMENT '业务ID',
  `command_status` tinyint NOT NULL COMMENT '命令状态',
  `request_payload_json` longtext NULL COMMENT '请求载荷',
  `failure_reason` varchar(1000) NULL COMMENT '失败原因',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`command_record_id`),
  UNIQUE KEY `uk_bpm_command_key` (`command_key`),
  KEY `idx_bpm_command_business` (`business_type`, `business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM命令执行记录';
