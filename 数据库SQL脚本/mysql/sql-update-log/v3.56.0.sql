-- BPM M4：核心运行时执行代、任务并发版本与 Graph 路由事实。
-- 执行前提：已执行 v3.54.0（M2 运行事实）和 v3.55.0（M3 数据治理）。
-- 兼容影响：仅新增有默认值的运行列；既有实例从执行代 1、既有任务从版本 1 继续。
-- 恢复方式：仅当没有新 M4 实例、任务或 Graph 路由事实时，方可删除新增列与索引。

SET @m4_instance_generation_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_bpm_instance' AND column_name = 'current_generation') = 1,
  'SELECT 1',
  'ALTER TABLE `t_bpm_instance` ADD COLUMN `current_generation` int NOT NULL DEFAULT 1 COMMENT ''当前执行代'' AFTER `active_task_count`'
);
PREPARE m4_instance_generation_stmt FROM @m4_instance_generation_sql;
EXECUTE m4_instance_generation_stmt;
DEALLOCATE PREPARE m4_instance_generation_stmt;

SET @m4_task_version_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_bpm_task' AND column_name = 'task_version') = 1,
  'SELECT 1',
  'ALTER TABLE `t_bpm_task` ADD COLUMN `task_version` bigint NOT NULL DEFAULT 1 COMMENT ''任务并发版本'' AFTER `task_result`'
);
PREPARE m4_task_version_stmt FROM @m4_task_version_sql;
EXECUTE m4_task_version_stmt;
DEALLOCATE PREPARE m4_task_version_stmt;

SET @m4_route_graph_version_sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 't_bpm_route_decision' AND column_name = 'graph_definition_version_id') = 1,
  'SELECT 1',
  'ALTER TABLE `t_bpm_route_decision`
     MODIFY COLUMN `definition_id` bigint NULL COMMENT ''旧定义ID，Graph路由为空'',
     MODIFY COLUMN `definition_node_id` bigint NULL COMMENT ''旧定义节点ID，Graph路由为空'',
     ADD COLUMN `graph_definition_version_id` bigint NULL COMMENT ''Graph定义版本ID'' AFTER `definition_id`,
     ADD KEY `idx_bpm_route_graph_version` (`graph_definition_version_id`)'
);
PREPARE m4_route_graph_version_stmt FROM @m4_route_graph_version_sql;
EXECUTE m4_route_graph_version_stmt;
DEALLOCATE PREPARE m4_route_graph_version_stmt;
