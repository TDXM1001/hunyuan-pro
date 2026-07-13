-- BPM M5：正式 Graph 高级运行事实与父子流程关系。
-- 执行前提：已执行 v3.54.0（Graph 定义与运行投影）和 v3.56.0（M4 核心运行时）。
-- 兼容影响：旧定义引用改为可空并新增 Graph 版本引用；既有时间事件和外部等待数据不改写。
-- 恢复方式：仅当没有 Graph 高级运行事实和父子流程关系时，方可删除新增表、列、索引及字典项；
--           definition_id/definition_node_id 恢复 NOT NULL 前必须确认所有旧数据均有对应引用。

ALTER TABLE `t_bpm_time_event`
  MODIFY COLUMN `definition_id` bigint NULL COMMENT '旧定义ID';

SET @m5_time_graph_version_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_bpm_time_event'
     AND column_name = 'graph_definition_version_id') = 1,
  'SELECT 1',
  'ALTER TABLE `t_bpm_time_event`
     ADD COLUMN `graph_definition_version_id` bigint NULL COMMENT ''Graph定义版本ID'' AFTER `definition_id`'
);
PREPARE m5_time_graph_version_stmt FROM @m5_time_graph_version_sql;
EXECUTE m5_time_graph_version_stmt;
DEALLOCATE PREPARE m5_time_graph_version_stmt;

SET @m5_time_graph_index_sql = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 't_bpm_time_event'
     AND index_name = 'idx_bpm_time_event_graph') > 0,
  'SELECT 1',
  'ALTER TABLE `t_bpm_time_event`
     ADD KEY `idx_bpm_time_event_graph` (`graph_definition_version_id`, `node_key`, `event_status`)'
);
PREPARE m5_time_graph_index_stmt FROM @m5_time_graph_index_sql;
EXECUTE m5_time_graph_index_stmt;
DEALLOCATE PREPARE m5_time_graph_index_stmt;

ALTER TABLE `t_bpm_external_wait`
  MODIFY COLUMN `definition_id` bigint NULL COMMENT '旧定义ID',
  MODIFY COLUMN `definition_node_id` bigint NULL COMMENT '旧定义节点ID';

SET @m5_wait_graph_version_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_bpm_external_wait'
     AND column_name = 'graph_definition_version_id') = 1,
  'SELECT 1',
  'ALTER TABLE `t_bpm_external_wait`
     ADD COLUMN `graph_definition_version_id` bigint NULL COMMENT ''Graph定义版本ID'' AFTER `definition_id`'
);
PREPARE m5_wait_graph_version_stmt FROM @m5_wait_graph_version_sql;
EXECUTE m5_wait_graph_version_stmt;
DEALLOCATE PREPARE m5_wait_graph_version_stmt;

SET @m5_wait_graph_index_sql = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 't_bpm_external_wait'
     AND index_name = 'idx_bpm_external_wait_graph') > 0,
  'SELECT 1',
  'ALTER TABLE `t_bpm_external_wait`
     ADD KEY `idx_bpm_external_wait_graph` (`graph_definition_version_id`, `node_key`, `wait_status`)'
);
PREPARE m5_wait_graph_index_stmt FROM @m5_wait_graph_index_sql;
EXECUTE m5_wait_graph_index_stmt;
DEALLOCATE PREPARE m5_wait_graph_index_stmt;

CREATE TABLE IF NOT EXISTS `t_bpm_sub_process_link` (
  `sub_process_link_id` bigint NOT NULL AUTO_INCREMENT COMMENT '父子流程关系ID',
  `event_key` varchar(160) NOT NULL COMMENT '稳定事件键',
  `parent_instance_id` bigint NOT NULL COMMENT '父实例ID',
  `parent_graph_definition_version_id` bigint NOT NULL COMMENT '父Graph定义版本ID',
  `parent_node_id` varchar(128) NOT NULL COMMENT '父 authored 节点ID',
  `parent_engine_execution_id` varchar(128) NOT NULL COMMENT '父等待执行ID',
  `child_instance_id` bigint NULL COMMENT '子实例ID',
  `child_engine_process_instance_id` varchar(128) NULL COMMENT '子引擎实例ID',
  `called_process_key` varchar(128) NOT NULL COMMENT '子流程key',
  `called_definition_version_id` bigint NOT NULL COMMENT '冻结子定义版本ID',
  `input_snapshot_json` longtext NOT NULL COMMENT '冻结输入映射结果',
  `output_snapshot_json` longtext NULL COMMENT '冻结输出映射结果',
  `failure_policy` varchar(32) NOT NULL COMMENT '失败传播策略',
  `cancel_propagation` varchar(32) NOT NULL COMMENT '取消传播策略',
  `link_status` varchar(32) NOT NULL COMMENT '关系状态',
  `last_error` varchar(2000) NULL COMMENT '最后错误',
  `started_at` datetime NULL COMMENT '子流程启动时间',
  `completed_at` datetime NULL COMMENT '子流程完成时间',
  `cancelled_at` datetime NULL COMMENT '取消时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`sub_process_link_id`),
  UNIQUE KEY `uk_bpm_sub_process_event` (`event_key`),
  KEY `idx_bpm_sub_process_parent` (`parent_instance_id`, `link_status`),
  KEY `idx_bpm_sub_process_child` (`child_instance_id`, `link_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM父子流程运行关系';

INSERT INTO `t_dict_data`
(`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`)
SELECT dict.`dict_id`, 'SUB_PROCESS', '调用子流程', 'SUB_PROCESS', 20, 0
FROM `t_dict` dict
WHERE dict.`dict_code` = 'BPM_PROCESS_NODE_TYPE'
  AND NOT EXISTS (
    SELECT 1 FROM `t_dict_data` data
    WHERE data.`dict_id` = dict.`dict_id` AND data.`data_value` = 'SUB_PROCESS'
  );

SET @m5_command_response_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_bpm_command_record'
     AND column_name = 'response_payload_json') = 1,
  'SELECT 1',
  'ALTER TABLE `t_bpm_command_record` ADD COLUMN `response_payload_json` longtext NULL COMMENT ''响应载荷'' AFTER `request_payload_json`'
);
PREPARE m5_command_response_stmt FROM @m5_command_response_sql;
EXECUTE m5_command_response_stmt;
DEALLOCATE PREPARE m5_command_response_stmt;

SET @m5_command_attempt_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_bpm_command_record'
     AND column_name = 'attempt_count') = 1,
  'SELECT 1',
  'ALTER TABLE `t_bpm_command_record` ADD COLUMN `attempt_count` int NOT NULL DEFAULT 0 COMMENT ''执行次数'' AFTER `response_payload_json`'
);
PREPARE m5_command_attempt_stmt FROM @m5_command_attempt_sql;
EXECUTE m5_command_attempt_stmt;
DEALLOCATE PREPARE m5_command_attempt_stmt;

SET @m5_command_retry_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_bpm_command_record'
     AND column_name = 'next_retry_at') = 1,
  'SELECT 1',
  'ALTER TABLE `t_bpm_command_record` ADD COLUMN `next_retry_at` datetime NULL COMMENT ''下次重试时间'' AFTER `attempt_count`'
);
PREPARE m5_command_retry_stmt FROM @m5_command_retry_sql;
EXECUTE m5_command_retry_stmt;
DEALLOCATE PREPARE m5_command_retry_stmt;
