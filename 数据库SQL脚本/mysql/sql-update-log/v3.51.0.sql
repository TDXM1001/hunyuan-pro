-- BPM M1 Graph：不可变定义版本与 authored/compiled 元素映射。
CREATE TABLE `t_bpm_graph_definition_version` (
  `graph_definition_version_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Graph定义版本ID',
  `draft_id` bigint NOT NULL COMMENT '来源草稿ID',
  `process_key` varchar(64) NOT NULL COMMENT '流程资产编码',
  `definition_version` int NOT NULL COMMENT '定义版本号',
  `lifecycle_state` varchar(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '版本状态',
  `graph_snapshot_json` longtext NOT NULL COMMENT '冻结Graph语义快照',
  `layout_snapshot_json` longtext NOT NULL COMMENT '冻结布局快照',
  `semantic_hash` char(64) NOT NULL COMMENT 'Graph语义摘要',
  `dependency_versions_json` longtext NOT NULL COMMENT '冻结跨模块依赖版本',
  `compiler_version` varchar(32) NOT NULL COMMENT 'Graph编译器版本',
  `compiled_bpmn_xml` longtext NOT NULL COMMENT '受控BPMN产物',
  `deployment_id` varchar(128) NOT NULL COMMENT 'Flowable部署ID',
  `engine_process_definition_id` varchar(128) NOT NULL COMMENT 'Flowable流程定义ID',
  `published_by_employee_id` bigint NOT NULL COMMENT '发布员工ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`graph_definition_version_id`),
  UNIQUE KEY `uk_bpm_graph_definition_version` (`process_key`, `definition_version`),
  KEY `idx_bpm_graph_definition_draft` (`draft_id`, `create_time`),
  KEY `idx_bpm_graph_definition_engine` (`engine_process_definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM Graph不可变定义版本';

CREATE TABLE `t_bpm_graph_definition_mapping` (
  `mapping_id` bigint NOT NULL AUTO_INCREMENT COMMENT '映射ID',
  `graph_definition_version_id` bigint NOT NULL COMMENT 'Graph定义版本ID',
  `authored_element_id` varchar(128) NOT NULL COMMENT '作者元素ID',
  `authored_element_kind` varchar(16) NOT NULL COMMENT '作者元素类型',
  `compiled_element_id` varchar(128) NOT NULL COMMENT 'BPMN元素ID',
  `compiled_element_type` varchar(32) NOT NULL COMMENT 'BPMN元素类型',
  PRIMARY KEY (`mapping_id`),
  UNIQUE KEY `uk_bpm_graph_mapping_authored` (`graph_definition_version_id`, `authored_element_id`),
  UNIQUE KEY `uk_bpm_graph_mapping_compiled` (`graph_definition_version_id`, `compiled_element_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM Graph作者与编译元素映射';
