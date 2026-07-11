-- BPM M1：路由决定事实与设计时抄送幂等键
CREATE TABLE `t_bpm_route_decision` (
  `route_decision_id` bigint NOT NULL AUTO_INCREMENT COMMENT '路由决定ID',
  `instance_id` bigint NOT NULL COMMENT 'Hunyuan流程实例ID',
  `definition_id` bigint NOT NULL COMMENT '流程定义ID',
  `definition_node_id` bigint NOT NULL COMMENT '路由定义节点ID',
  `engine_process_instance_id` varchar(64) NOT NULL COMMENT 'Flowable实例代际ID',
  `route_node_key` varchar(128) NOT NULL COMMENT 'authored路由节点key',
  `input_form_data_version` bigint NOT NULL COMMENT '参与计算的表单数据版本',
  `matched_branch_keys_json` longtext NOT NULL COMMENT '按authored顺序命中的分支key',
  `default_branch_used` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否使用默认分支',
  `evaluation_status` varchar(16) NOT NULL COMMENT '计算状态',
  `reason_snapshot_json` longtext NOT NULL COMMENT '可审计计算原因快照',
  `evaluated_at` datetime NOT NULL COMMENT '计算时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`route_decision_id`),
  UNIQUE KEY `uk_bpm_route_generation_node` (`instance_id`,`engine_process_instance_id`,`route_node_key`),
  KEY `idx_bpm_route_instance` (`instance_id`,`route_decision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM路由决定事实';

ALTER TABLE `t_bpm_instance_copy`
  ADD COLUMN `source_event_key` varchar(191) NULL COMMENT '设计时抄送幂等事件key' AFTER `source_node_name`,
  ADD UNIQUE KEY `uk_bpm_copy_source_target` (`instance_id`,`engine_process_instance_id`,`source_event_key`,`target_employee_id`);
