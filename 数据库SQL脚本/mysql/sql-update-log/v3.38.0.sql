-- BPM P1.1：流程定义可发起范围治理
ALTER TABLE `t_bpm_definition`
    ADD COLUMN `start_scope_json` longtext NULL COMMENT '可发起范围快照JSON' AFTER `start_state`;

CREATE INDEX `idx_definition_start_state` ON `t_bpm_definition` (`start_state`);
