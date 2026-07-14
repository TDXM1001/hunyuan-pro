-- BPM M8：定义演进、影响预演与受限实例迁移。
-- 执行前提：已执行 v3.59.0，M1 Graph 稳定 ID/版本快照、M5 运行事实、M7 处置审计均已启用。
-- 兼容影响：仅新增 M8 迁移事实表、菜单与权限；发布新版本仍只影响新实例，不修改 Flowable 表结构。
-- 恢复方式：停止新的迁移预演与确认，保留批次/明细审计备份；移除菜单权限。已成功迁移的实例不得通过删表物理回滚，只能按 M7 补偿或重新发起。
CREATE TABLE IF NOT EXISTS `t_bpm_migration_batch` (
  `migration_batch_id` bigint NOT NULL AUTO_INCREMENT,
  `batch_code` varchar(160) NOT NULL,
  `idempotency_key` varchar(128) NOT NULL,
  `source_version_id` bigint NOT NULL,
  `target_version_id` bigint NOT NULL,
  `batch_status` varchar(32) NOT NULL,
  `mapping_json` longtext NOT NULL,
  `data_mapping_json` longtext NOT NULL,
  `diff_snapshot_json` longtext NULL,
  `reason` varchar(512) NOT NULL,
  `actor_employee_id` bigint NOT NULL,
  `confirmed_by_employee_id` bigint NULL,
  `execution_owner_key` varchar(64) NULL,
  `execution_lease_until` datetime NULL,
  `total_count` int NOT NULL DEFAULT 0,
  `eligible_count` int NOT NULL DEFAULT 0,
  `blocked_count` int NOT NULL DEFAULT 0,
  `succeeded_count` int NOT NULL DEFAULT 0,
  `failed_count` int NOT NULL DEFAULT 0,
  `previewed_at` datetime NOT NULL,
  `confirmed_at` datetime NULL,
  `completed_at` datetime NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`migration_batch_id`),
  UNIQUE KEY `uk_bpm_migration_batch_code` (`batch_code`),
  UNIQUE KEY `uk_bpm_migration_batch_idempotency` (`idempotency_key`),
  KEY `idx_bpm_migration_batch_versions` (`source_version_id`,`target_version_id`,`batch_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM M8 受限迁移批次';
CREATE TABLE IF NOT EXISTS `t_bpm_migration_item` (
  `migration_item_id` bigint NOT NULL AUTO_INCREMENT,
  `migration_batch_id` bigint NOT NULL,
  `instance_id` bigint NOT NULL,
  `idempotency_key` varchar(160) NOT NULL,
  `item_status` varchar(32) NOT NULL,
  `blockers_json` longtext NOT NULL,
  `source_snapshot_json` longtext NOT NULL,
  `target_snapshot_json` longtext NULL,
  `engine_command_evidence_json` longtext NULL,
  `failure_reason` varchar(1000) NULL,
  `compensation_result` varchar(512) NULL,
  `executed_by_employee_id` bigint NULL,
  `disposition_by_employee_id` bigint NULL,
  `disposed_at` datetime NULL,
  `migrated_at` datetime NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`migration_item_id`),
  UNIQUE KEY `uk_bpm_migration_item_idempotency` (`idempotency_key`),
  UNIQUE KEY `uk_bpm_migration_item_instance` (`migration_batch_id`,`instance_id`),
  KEY `idx_bpm_migration_item_status` (`migration_batch_id`,`item_status`,`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM M8 迁移实例结果与前后审计';
INSERT INTO `t_menu` (`menu_id`,`menu_name`,`menu_type`,`parent_id`,`sort`,`path`,`component`,`perms_type`,`api_perms`,`web_perms`,`icon`,`frame_flag`,`cache_flag`,`visible_flag`,`disabled_flag`,`deleted_flag`,`create_user_id`,`create_time`,`update_time`) VALUES
(382,'迁移与演进',2,308,25,'/system/bpm/evolution/workbench','/system/bpm/evolution/workbench.vue',1,NULL,NULL,'ep:refresh',0,0,1,0,0,1,now(),now()),
(383,'查询迁移演进',3,382,1,NULL,NULL,1,'bpm:evolution:query','bpm:evolution:query',NULL,0,0,1,0,0,1,now(),now()),
(384,'预演实例迁移',3,382,2,NULL,NULL,1,'bpm:evolution:preview','bpm:evolution:preview',NULL,0,0,1,0,0,1,now(),now()),
(385,'确认实例迁移',3,382,3,NULL,NULL,1,'bpm:evolution:execute','bpm:evolution:execute',NULL,0,0,1,0,0,1,now(),now()),
(386,'审计实例迁移',3,382,4,NULL,NULL,1,'bpm:evolution:audit','bpm:evolution:audit',NULL,0,0,1,0,0,1,now(),now())
ON DUPLICATE KEY UPDATE `menu_name`=VALUES(`menu_name`),`component`=VALUES(`component`),`api_perms`=VALUES(`api_perms`),`web_perms`=VALUES(`web_perms`),`update_time`=now();
INSERT INTO `t_role_menu` (`role_id`,`menu_id`,`create_time`,`update_time`)
SELECT 1,m.menu_id,now(),now() FROM t_menu m WHERE m.menu_id IN (382,383,384,385,386)
AND NOT EXISTS (SELECT 1 FROM t_role_menu rm WHERE rm.role_id=1 AND rm.menu_id=m.menu_id);
