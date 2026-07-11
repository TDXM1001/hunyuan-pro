-- BPM 审批数据治理：实例数据版本、字段变更账本与样板费用最终值
ALTER TABLE `t_bpm_instance`
  ADD COLUMN `form_data_version` bigint NOT NULL DEFAULT 1 COMMENT '当前表单数据版本' AFTER `current_form_data_snapshot_json`;

CREATE TABLE `t_bpm_form_data_change` (
  `change_id` bigint NOT NULL AUTO_INCREMENT COMMENT '变更ID',
  `instance_id` bigint NOT NULL COMMENT 'Hunyuan流程实例ID',
  `task_id` bigint NULL COMMENT 'Hunyuan流程任务ID',
  `definition_node_id` bigint NULL COMMENT '定义节点快照ID',
  `node_key_snapshot` varchar(128) NULL COMMENT '节点标识快照',
  `change_source` varchar(32) NOT NULL COMMENT '变更来源',
  `actor_employee_id` bigint NOT NULL COMMENT '操作员工ID',
  `actor_name_snapshot` varchar(100) NOT NULL COMMENT '操作员工姓名快照',
  `before_version` bigint NOT NULL COMMENT '修改前版本，首次发起为0',
  `after_version` bigint NOT NULL COMMENT '修改后版本',
  `changed_fields_json` longtext NOT NULL COMMENT '实际变化字段key数组',
  `before_values_json` longtext NOT NULL COMMENT '变化字段修改前值',
  `after_values_json` longtext NOT NULL COMMENT '变化字段修改后值',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`change_id`),
  KEY `idx_bpm_form_change_instance` (`instance_id`, `change_id`),
  KEY `idx_bpm_form_change_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM表单数据变更记录';

ALTER TABLE `t_bpm_sample_expense`
  ADD COLUMN `approved_amount` decimal(12,2) NULL COMMENT '审批核定金额' AFTER `amount`,
  ADD COLUMN `final_form_data_version` bigint NULL COMMENT '最终流程表单数据版本' AFTER `callback_event_id`;
