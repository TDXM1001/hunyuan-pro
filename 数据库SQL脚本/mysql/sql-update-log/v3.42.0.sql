-- BPM P2.4：业务样板费用申请
CREATE TABLE `t_bpm_sample_expense` (
  `expense_id` bigint NOT NULL AUTO_INCREMENT COMMENT '样板费用申请ID',
  `title` varchar(100) NOT NULL COMMENT '申请标题',
  `amount` decimal(12,2) NOT NULL COMMENT '申请金额',
  `applicant_employee_id` bigint NOT NULL COMMENT '申请人员工ID',
  `approval_status` int NOT NULL COMMENT '业务审批状态：0草稿 1审批中 2已通过 3已拒绝',
  `instance_id` bigint NULL COMMENT '关联BPM实例ID',
  `callback_event_id` varchar(100) NULL COMMENT '最近一次回调事件ID',
  `callback_fail_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '下一次回调是否故意失败',
  `approved_at` datetime NULL COMMENT '审批通过回写时间',
  `rejected_at` datetime NULL COMMENT '审批拒绝回写时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`expense_id`),
  KEY `idx_bpm_sample_expense_instance` (`instance_id`),
  KEY `idx_bpm_sample_expense_applicant` (`applicant_employee_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM样板费用申请';
