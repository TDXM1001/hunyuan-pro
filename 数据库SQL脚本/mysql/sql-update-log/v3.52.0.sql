-- BPM M1 发布期依赖：M2 候选策略与 M3 业务契约的不可变版本目录。
-- 定义发布只能冻结本脚本建立的 ACTIVE 版本，不接受调用方提交的任意版本号。

CREATE TABLE IF NOT EXISTS `t_bpm_candidate_policy_version` (
  `candidate_policy_version_id` bigint NOT NULL AUTO_INCREMENT COMMENT '候选策略版本ID',
  `policy_key` varchar(64) NOT NULL COMMENT '候选策略编码',
  `policy_version` int NOT NULL COMMENT '候选策略版本号',
  `lifecycle_state` varchar(16) NOT NULL COMMENT '版本状态',
  `policy_json` longtext NOT NULL COMMENT '冻结候选与完成策略',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`candidate_policy_version_id`),
  UNIQUE KEY `uk_bpm_candidate_policy_version` (`policy_key`, `policy_version`),
  KEY `idx_bpm_candidate_policy_lifecycle` (`lifecycle_state`, `policy_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM候选策略不可变版本目录';

CREATE TABLE IF NOT EXISTS `t_bpm_business_contract_version` (
  `business_contract_version_id` bigint NOT NULL AUTO_INCREMENT COMMENT '业务契约版本ID',
  `contract_key` varchar(64) NOT NULL COMMENT '业务契约编码',
  `contract_version` int NOT NULL COMMENT '业务契约版本号',
  `lifecycle_state` varchar(16) NOT NULL COMMENT '版本状态',
  `contract_json` longtext NOT NULL COMMENT '冻结业务对象与字段契约',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`business_contract_version_id`),
  UNIQUE KEY `uk_bpm_business_contract_version` (`contract_key`, `contract_version`),
  KEY `idx_bpm_business_contract_lifecycle` (`lifecycle_state`, `contract_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='BPM业务契约不可变版本目录';

-- Graph 定义的发布与下线是独立高风险动作，权限点不能借用旧模型发布权限。
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
VALUES
  (339, '发布Graph定义', 3, 311, 15, NULL, NULL, 1, 'bpm:graph-definition:publish', 'bpm:graph-definition:publish', NULL, 311, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (340, '下线Graph定义', 3, 311, 16, NULL, NULL, 1, 'bpm:graph-definition:deactivate', 'bpm:graph-definition:deactivate', NULL, 311, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `parent_id` = VALUES(`parent_id`), `sort` = VALUES(`sort`),
  `api_perms` = VALUES(`api_perms`), `web_perms` = VALUES(`web_perms`),
  `context_menu_id` = VALUES(`context_menu_id`), `visible_flag` = VALUES(`visible_flag`),
  `disabled_flag` = VALUES(`disabled_flag`), `deleted_flag` = VALUES(`deleted_flag`), `update_time` = now();

INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, menu.`menu_id`, now(), now()
FROM `t_menu` menu
WHERE menu.`menu_id` IN (339, 340)
  AND menu.`deleted_flag` = 0
  AND menu.`disabled_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `t_role_menu` role_menu
    WHERE role_menu.`role_id` = 1 AND role_menu.`menu_id` = menu.`menu_id`
  );
