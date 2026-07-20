-- Retire the BPM/Flowable capability after taking an external backup.
-- Prerequisite: stop application traffic and archive all workflow data required by audit policy.
-- Compatibility: removes BPM menus, permissions, dictionaries, support data, and runtime tables.
-- Recovery: restore from the pre-removal database backup and deploy the archived BPM code baseline.

DELETE role_menu
FROM `t_role_menu` role_menu
JOIN `t_menu` menu ON menu.`menu_id` = role_menu.`menu_id`
WHERE menu.`menu_id` = 308
   OR LOWER(COALESCE(menu.`path`, '')) LIKE '%/bpm%'
   OR LOWER(COALESCE(menu.`component`, '')) LIKE '%/bpm/%'
   OR LOWER(COALESCE(menu.`api_perms`, '')) LIKE 'bpm:%'
   OR LOWER(COALESCE(menu.`web_perms`, '')) LIKE 'bpm:%';

DELETE FROM `t_menu`
WHERE `menu_id` = 308
   OR LOWER(COALESCE(`path`, '')) LIKE '%/bpm%'
   OR LOWER(COALESCE(`component`, '')) LIKE '%/bpm/%'
   OR LOWER(COALESCE(`api_perms`, '')) LIKE 'bpm:%'
   OR LOWER(COALESCE(`web_perms`, '')) LIKE 'bpm:%';

DELETE role_employee
FROM `t_role_employee` role_employee
JOIN `t_role` role_record ON role_record.`role_id` = role_employee.`role_id`
WHERE role_record.`role_code` = 'bpm_runtime_user';

DELETE FROM `t_role_menu`
WHERE `role_id` IN (
    SELECT `role_id` FROM `t_role` WHERE `role_code` = 'bpm_runtime_user'
);

DELETE FROM `t_role` WHERE `role_code` = 'bpm_runtime_user';

DELETE dict_data
FROM `t_dict_data` dict_data
JOIN `t_dict` dict_record ON dict_record.`dict_id` = dict_data.`dict_id`
WHERE UPPER(dict_record.`dict_code`) LIKE 'BPM\_%';

DELETE FROM `t_dict` WHERE UPPER(`dict_code`) LIKE 'BPM\_%';

SET @previous_foreign_key_checks = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS
    `t_bpm_approval_command_receipt`,
    `t_bpm_approval_group`,
    `t_bpm_approval_policy_version`,
    `t_bpm_approval_stage`,
    `t_bpm_approval_stage_member`,
    `t_bpm_approval_subject_snapshot`,
    `t_bpm_business_contract_version`,
    `t_bpm_callback_record`,
    `t_bpm_candidate_policy_version`,
    `t_bpm_category`,
    `t_bpm_command_record`,
    `t_bpm_connector_definition`,
    `t_bpm_definition`,
    `t_bpm_definition_node`,
    `t_bpm_event_subscription_version`,
    `t_bpm_external_employee_mapping`,
    `t_bpm_external_public_reference`,
    `t_bpm_external_request_nonce`,
    `t_bpm_external_wait`,
    `t_bpm_form`,
    `t_bpm_form_data_change`,
    `t_bpm_graph_definition_mapping`,
    `t_bpm_graph_definition_version`,
    `t_bpm_instance`,
    `t_bpm_instance_copy`,
    `t_bpm_migration_batch`,
    `t_bpm_migration_item`,
    `t_bpm_model`,
    `t_bpm_notification_record`,
    `t_bpm_operations_action_log`,
    `t_bpm_operations_case`,
    `t_bpm_operations_retention_policy`,
    `t_bpm_process_binding_version`,
    `t_bpm_process_draft`,
    `t_bpm_process_template`,
    `t_bpm_process_working_data`,
    `t_bpm_route_decision`,
    `t_bpm_routing_fact_snapshot`,
    `t_bpm_sample_expense`,
    `t_bpm_source_application`,
    `t_bpm_source_system_version`,
    `t_bpm_start_visibility_policy_version`,
    `t_bpm_sub_process_link`,
    `t_bpm_task`,
    `t_bpm_task_action_evidence`,
    `t_bpm_task_action_log`,
    `t_bpm_time_event`,
    `act_evt_log`,
    `act_ge_bytearray`,
    `act_ge_property`,
    `act_hi_actinst`,
    `act_hi_attachment`,
    `act_hi_comment`,
    `act_hi_detail`,
    `act_hi_entitylink`,
    `act_hi_identitylink`,
    `act_hi_procinst`,
    `act_hi_taskinst`,
    `act_hi_tsk_log`,
    `act_hi_varinst`,
    `act_procdef_info`,
    `act_re_deployment`,
    `act_re_model`,
    `act_re_procdef`,
    `act_ru_actinst`,
    `act_ru_deadletter_job`,
    `act_ru_entitylink`,
    `act_ru_event_subscr`,
    `act_ru_execution`,
    `act_ru_external_job`,
    `act_ru_history_job`,
    `act_ru_identitylink`,
    `act_ru_job`,
    `act_ru_suspended_job`,
    `act_ru_task`,
    `act_ru_timer_job`,
    `act_ru_variable`,
    `t_user_group_employee`,
    `t_user_group`,
    `t_employee_reporting_relation`;

SET FOREIGN_KEY_CHECKS = @previous_foreign_key_checks;
