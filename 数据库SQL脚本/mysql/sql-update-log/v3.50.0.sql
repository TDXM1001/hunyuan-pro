-- BPM M1 Graph：正式作者草稿与模板资产，独立于旧 SimpleModel 表。
CREATE TABLE `t_bpm_process_draft` (
  `draft_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Graph 草稿 ID',
  `process_key` varchar(64) NOT NULL COMMENT '流程资产编码',
  `process_name` varchar(128) NOT NULL COMMENT '流程资产名称',
  `category_id` bigint NULL COMMENT '流程分类 ID',
  `revision` int NOT NULL COMMENT '乐观锁版本',
  `graph_json` longtext NOT NULL COMMENT '不含布局的 canonical Graph',
  `layout_json` longtext NOT NULL COMMENT '按节点 ID 保存的布局',
  `semantic_hash` char(64) NOT NULL COMMENT 'Graph SHA-256 语义摘要',
  `draft_status` varchar(32) NOT NULL COMMENT '草稿状态',
  `created_by_employee_id` bigint NOT NULL COMMENT '创建员工 ID',
  `updated_by_employee_id` bigint NOT NULL COMMENT '最后更新员工 ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`draft_id`),
  UNIQUE KEY `uk_bpm_process_draft_key` (`process_key`),
  KEY `idx_bpm_process_draft_category` (`category_id`, `draft_status`),
  KEY `idx_bpm_process_draft_hash` (`semantic_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM正式Graph流程草稿';

CREATE TABLE `t_bpm_process_template` (
  `template_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板 ID',
  `template_key` varchar(64) NOT NULL COMMENT '模板编码',
  `template_name` varchar(128) NOT NULL COMMENT '模板名称',
  `category_id` bigint NULL COMMENT '流程分类 ID',
  `source_draft_id` bigint NULL COMMENT '来源草稿 ID',
  `graph_json` longtext NOT NULL COMMENT '不含布局的 canonical Graph',
  `layout_json` longtext NOT NULL COMMENT '按节点 ID 保存的布局',
  `semantic_hash` char(64) NOT NULL COMMENT 'Graph SHA-256 语义摘要',
  `enabled_flag` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可复制',
  `created_by_employee_id` bigint NOT NULL COMMENT '创建员工 ID',
  `updated_by_employee_id` bigint NOT NULL COMMENT '最后更新员工 ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_bpm_process_template_key` (`template_key`),
  KEY `idx_bpm_process_template_category` (`category_id`, `enabled_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM正式Graph流程模板';

-- Graph 草稿和模板接口权限；前端设计器页面由后续 M1 批次接入。
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
VALUES
  (334, '查看Graph草稿', 3, 311, 10, NULL, NULL, 1, 'bpm:graph-draft:detail', 'bpm:graph-draft:detail', NULL, 311, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (335, '新增Graph草稿', 3, 311, 11, NULL, NULL, 1, 'bpm:graph-draft:add', 'bpm:graph-draft:add', NULL, 311, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (336, '保存Graph草稿', 3, 311, 12, NULL, NULL, 1, 'bpm:graph-draft:update', 'bpm:graph-draft:update', NULL, 311, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (337, '冻结Graph模板', 3, 311, 13, NULL, NULL, 1, 'bpm:graph-template:add', 'bpm:graph-template:add', NULL, 311, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (338, '复制Graph模板', 3, 311, 14, NULL, NULL, 1, 'bpm:graph-template:copy', 'bpm:graph-template:copy', NULL, 311, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `parent_id` = VALUES(`parent_id`), `sort` = VALUES(`sort`),
  `api_perms` = VALUES(`api_perms`), `web_perms` = VALUES(`web_perms`),
  `context_menu_id` = VALUES(`context_menu_id`), `visible_flag` = VALUES(`visible_flag`),
  `disabled_flag` = VALUES(`disabled_flag`), `deleted_flag` = VALUES(`deleted_flag`), `update_time` = now();

INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, menu.`menu_id`, now(), now()
FROM `t_menu` menu
WHERE menu.`menu_id` IN (334, 335, 336, 337, 338)
  AND menu.`deleted_flag` = 0
  AND menu.`disabled_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `t_role_menu` role_menu
    WHERE role_menu.`role_id` = 1 AND role_menu.`menu_id` = menu.`menu_id`
  );
