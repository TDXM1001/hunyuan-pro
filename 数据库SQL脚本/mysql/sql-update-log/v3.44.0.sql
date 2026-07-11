-- BPM 多人审批组：将既有并行专属数据库注释泛化为顺序/并行共用语义
ALTER TABLE `t_bpm_approval_group`
  MODIFY COLUMN `approval_group_key` varchar(128) NOT NULL COMMENT 'authored审批节点业务标识',
  MODIFY COLUMN `approval_group_name` varchar(255) NOT NULL COMMENT 'authored审批节点名称快照',
  COMMENT = 'BPM多人审批组';

ALTER TABLE `t_bpm_task`
  MODIFY COLUMN `approval_group_id` bigint NULL COMMENT '多人审批组ID';
