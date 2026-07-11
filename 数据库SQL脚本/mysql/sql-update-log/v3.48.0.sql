-- 流程业务投影需先取得 instance_id，再启动 Flowable 并回填引擎实例ID。
ALTER TABLE `t_bpm_instance`
  MODIFY COLUMN `engine_process_instance_id` varchar(128) NULL COMMENT '引擎实例ID';
