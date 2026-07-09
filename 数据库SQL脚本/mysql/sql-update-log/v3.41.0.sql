-- BPM P2.3：业务回调人工补偿审计
ALTER TABLE `t_bpm_callback_record`
    ADD COLUMN `compensated_at` datetime NULL COMMENT '人工补偿时间' AFTER `next_retry_at`,
    ADD COLUMN `compensated_by` bigint NULL COMMENT '人工补偿操作人ID' AFTER `compensated_at`,
    ADD COLUMN `compensation_reason` varchar(500) NULL COMMENT '人工补偿说明' AFTER `compensated_by`;
