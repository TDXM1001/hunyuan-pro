-- BPM 并行全员会签：审批组运行时事实与成员任务关联
CREATE TABLE `t_bpm_approval_group` (
  `approval_group_id` bigint NOT NULL AUTO_INCREMENT COMMENT '审批组ID',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `definition_id` bigint NOT NULL COMMENT '流程定义ID',
  `engine_process_instance_id` varchar(128) NOT NULL COMMENT 'Flowable流程实例ID',
  `approval_group_key` varchar(128) NOT NULL COMMENT '会签节点业务标识',
  `approval_group_name` varchar(255) NOT NULL COMMENT '会签节点名称快照',
  `approval_mode` varchar(32) NOT NULL COMMENT '审批模式',
  `group_state` varchar(32) NOT NULL COMMENT '审批组状态',
  `close_reason` varchar(64) NULL COMMENT '关闭原因',
  `total_member_count` int NOT NULL COMMENT '成员总数',
  `processed_member_count` int NOT NULL DEFAULT 0 COMMENT '已处理成员数',
  `approved_member_count` int NOT NULL DEFAULT 0 COMMENT '已通过成员数',
  `rejected_member_count` int NOT NULL DEFAULT 0 COMMENT '已拒绝成员数',
  `closed_at` datetime NULL COMMENT '关闭时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`approval_group_id`),
  UNIQUE KEY `uk_bpm_approval_group_engine_key` (`engine_process_instance_id`, `approval_group_key`),
  KEY `idx_bpm_approval_group_instance_state` (`instance_id`, `group_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM并行审批组';

ALTER TABLE `t_bpm_task`
  ADD COLUMN `approval_group_id` bigint NULL COMMENT '并行审批组ID' AFTER `definition_node_id`,
  ADD KEY `idx_bpm_task_approval_group_state` (`approval_group_id`, `task_state`);
