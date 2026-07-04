CREATE TABLE IF NOT EXISTS `t_sms_template` (
  `template_code` varchar(100) NOT NULL COMMENT 'template code',
  `template_name` varchar(100) NOT NULL COMMENT 'template name',
  `template_content` text NOT NULL COMMENT 'template content',
  `disable_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT 'disabled flag',
  `remark` varchar(500) DEFAULT NULL COMMENT 'remark',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  PRIMARY KEY (`template_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sms template';

CREATE TABLE IF NOT EXISTS `t_sms_send_log` (
  `sms_send_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `provider` varchar(50) DEFAULT NULL COMMENT 'provider name',
  `request_id` varchar(100) DEFAULT NULL COMMENT 'provider request id',
  `phone` varchar(30) NOT NULL COMMENT 'phone number',
  `template_code` varchar(100) NOT NULL COMMENT 'template code',
  `template_content` text DEFAULT NULL COMMENT 'template content',
  `template_params` text DEFAULT NULL COMMENT 'template params json',
  `send_content` text NOT NULL COMMENT 'send content',
  `idempotent_key` varchar(100) DEFAULT NULL COMMENT 'idempotent key',
  `send_status` tinyint NOT NULL COMMENT '0 pending, 1 success, 2 fail',
  `fail_reason` varchar(500) DEFAULT NULL COMMENT 'fail reason',
  `send_time` datetime DEFAULT NULL COMMENT 'send time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  PRIMARY KEY (`sms_send_log_id`) USING BTREE,
  KEY `idx_sms_send_phone_template` (`phone`, `template_code`) USING BTREE,
  KEY `idx_sms_send_status_time` (`send_status`, `create_time`) USING BTREE,
  KEY `idx_sms_send_idempotent` (`idempotent_key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='sms send log';

INSERT INTO `t_sms_template` (`template_code`, `template_name`, `template_content`, `disable_flag`, `remark`)
VALUES ('login_verification_code', 'login verification code', 'Your verification code is ${code}. It expires in ${minutes} minutes.', b'0', 'default login verification sms template')
ON DUPLICATE KEY UPDATE
  `template_name` = VALUES(`template_name`),
  `remark` = VALUES(`remark`);

INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
VALUES
  (301, 'SMS template query', 3, 50, NULL, NULL, NULL, 1, 'support:sms:template:query', 'support:sms:template:query', NULL, NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (302, 'SMS template add', 3, 50, NULL, NULL, NULL, 1, 'support:sms:template:add', 'support:sms:template:add', NULL, NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (303, 'SMS template update', 3, 50, NULL, NULL, NULL, 1, 'support:sms:template:update', 'support:sms:template:update', NULL, NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now()),
  (304, 'SMS send log query', 3, 50, NULL, NULL, NULL, 1, 'support:sms:sendLog:query', 'support:sms:sendLog:query', NULL, NULL, 0, NULL, 0, 1, 0, 0, 1, now(), NULL, now())
ON DUPLICATE KEY UPDATE
  `api_perms` = VALUES(`api_perms`),
  `web_perms` = VALUES(`web_perms`),
  `update_time` = now();
