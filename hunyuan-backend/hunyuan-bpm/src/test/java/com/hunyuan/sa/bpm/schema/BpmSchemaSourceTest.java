package com.hunyuan.sa.bpm.schema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BpmSchemaSourceTest {

    @Test
    void allowsBpmInstanceToBindFlowableInstanceAfterBusinessProjectionInsert() throws IOException {
        String sql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.48.0.sql"));

        assertThat(sql).contains("ALTER TABLE `t_bpm_instance`");
        assertThat(sql).contains("MODIFY COLUMN `engine_process_instance_id` varchar(128) NULL");
    }

    @Test
    void definesIdempotentBpmDictionariesForM1AndExistingRuntimeEnums() throws IOException {
        String sql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.47.0.sql"));

        assertThat(sql).contains("BPM_PROCESS_NODE_TYPE", "BPM_BRANCH_TYPE", "BPM_TASK_KIND");
        assertThat(sql).contains("BPM_TASK_ACTION", "BPM_RUNTIME_NODE_STATE", "BPM_ROUTE_EVALUATION_STATUS");
        assertThat(sql).contains("BPM_CANDIDATE_RESOLVER_TYPE", "BPM_TASK_RESULT", "BPM_COPY_TYPE");
        assertThat(sql).contains("BPM_FORM_TYPE", "BPM_APPROVAL_MODE", "BPM_TASK_ACTION_LOG_TYPE");
        assertThat(sql).contains("BPM_COMMAND_TYPE", "BPM_COMMAND_STATUS");
        assertThat(sql).contains("'HANDLE_TASK'", "'COPY_TASK'", "'HANDLED'", "'DESIGN_NODE_COPY'");
        assertThat(sql).contains("'parallelAll'", "'START'", "'HANDLE_COMPLETED'", "'RESUBMITTED'");
        assertThat(sql).contains("LEFT JOIN `t_dict_data` existing");
        assertThat(sql).contains("existing.`dict_data_id` IS NULL");
    }

    @Test
    void definesM1RouteDecisionLedgerAndCopyIdempotencyKey() throws IOException {
        String sql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.46.0.sql"));

        assertThat(sql).contains("CREATE TABLE `t_bpm_route_decision`");
        assertThat(sql).contains("`engine_process_instance_id` varchar(64) NOT NULL");
        assertThat(sql).contains("UNIQUE KEY `uk_bpm_route_generation_node`");
        assertThat(sql).contains("`instance_id`,`engine_process_instance_id`,`route_node_key`");
        assertThat(sql).contains("ADD COLUMN `source_event_key`");
        assertThat(sql).contains("UNIQUE KEY `uk_bpm_copy_source_target`");
    }

    @Test
    void definesTheNineCoreBpmTablesAndProjectionColumns() throws IOException {
        String sql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.34.0.sql"));

        assertThat(sql).contains("CREATE TABLE `t_bpm_category`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_form`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_model`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_definition`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_definition_node`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_instance`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_task`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_task_action_log`");
        assertThat(sql).contains("CREATE TABLE `t_bpm_instance_copy`");
        assertThat(sql).contains("initial_form_data_snapshot_json");
        assertThat(sql).contains("current_form_data_snapshot_json");
        assertThat(sql).contains("compiled_bpmn_xml");
        assertThat(sql).contains("compiled_node_snapshot_json");
    }

    @Test
    void keepsEntitiesForDefinitionAndRuntimeSnapshots() throws IOException {
        String definitionEntity = Files.readString(Path.of("src/main/java/com/hunyuan/sa/bpm/module/definition/domain/entity/BpmDefinitionEntity.java"));
        String instanceEntity = Files.readString(Path.of("src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmInstanceEntity.java"));
        String taskEntity = Files.readString(Path.of("src/main/java/com/hunyuan/sa/bpm/module/runtime/domain/entity/BpmTaskEntity.java"));

        assertThat(definitionEntity).contains("private String compiledBpmnXml;");
        assertThat(definitionEntity).contains("private Integer lifecycleState;");
        assertThat(instanceEntity).contains("private String initialFormDataSnapshotJson;");
        assertThat(instanceEntity).contains("private String currentFormDataSnapshotJson;");
        assertThat(taskEntity).contains("private String engineTaskId;");
        assertThat(taskEntity).contains("private Long assigneeEmployeeId;");
    }

    @Test
    void keepsFlowableHiddenKernelEnabledInAllRuntimeProfiles() throws IOException {
        String devYaml = Files.readString(Path.of("src/main/resources/dev/hunyuan-bpm.yaml"));
        String testYaml = Files.readString(Path.of("src/main/resources/test/hunyuan-bpm.yaml"));
        String preYaml = Files.readString(Path.of("src/main/resources/pre/hunyuan-bpm.yaml"));
        String prodYaml = Files.readString(Path.of("src/main/resources/prod/hunyuan-bpm.yaml"));

        assertThat(devYaml).contains("enabled: true");
        assertThat(testYaml).contains("enabled: true");
        assertThat(preYaml).contains("enabled: true");
        assertThat(prodYaml).contains("enabled: true");
    }

    @Test
    void keepsDevProfileReadyToBootstrapFlowableKernelTablesForLocalStartup() throws IOException {
        String devYaml = Files.readString(Path.of("src/main/resources/dev/hunyuan-bpm.yaml"));

        assertThat(devYaml).contains("database-schema-update: true");
    }

    @Test
    void activatesFlowableAsyncExecutorOutsideTestsForM4Timers() throws IOException {
        String devYaml = Files.readString(Path.of("src/main/resources/dev/hunyuan-bpm.yaml"));
        String testYaml = Files.readString(Path.of("src/main/resources/test/hunyuan-bpm.yaml"));
        String preYaml = Files.readString(Path.of("src/main/resources/pre/hunyuan-bpm.yaml"));
        String prodYaml = Files.readString(Path.of("src/main/resources/prod/hunyuan-bpm.yaml"));

        assertThat(devYaml).contains("async-executor-activate: true");
        assertThat(preYaml).contains("async-executor-activate: true");
        assertThat(prodYaml).contains("async-executor-activate: true");
        assertThat(testYaml).contains("async-executor-activate: false");
    }

    @Test
    void definesM4TimeEventWaitConnectorTablesAndIdempotentDictionaries() throws IOException {
        String sql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.49.0.sql"));

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `t_bpm_time_event`");
        assertThat(sql).contains("UNIQUE KEY `uk_bpm_time_event_key` (`event_key`)");
        assertThat(sql).contains("KEY `idx_bpm_time_event_schedule` (`event_status`, `scheduled_at`)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `t_bpm_external_wait`");
        assertThat(sql).contains("UNIQUE KEY `uk_bpm_external_wait_correlation` (`correlation_key`)");
        assertThat(sql).contains("UNIQUE KEY `uk_bpm_external_wait_token` (`callback_token_hash`)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `t_bpm_connector_definition`");
        assertThat(sql).contains("UNIQUE KEY `uk_bpm_connector_version` (`connector_key`, `connector_version`)");
        assertThat(sql).contains("BPM_TIME_EVENT_STATUS", "BPM_EXTERNAL_WAIT_STATUS", "BPM_TIMEOUT_ACTION");
        assertThat(sql).contains("'DELAY'", "'EXTERNAL_TRIGGER'", "'AUTO_APPROVE'", "'WAIT_CALLBACK'");
        assertThat(sql).contains("LEFT JOIN `t_dict_data` existing");
        assertThat(sql).contains("existing.`dict_data_id` IS NULL");
    }

    @Test
    void upgradesAdvancedRuntimeFactsForGraphAndSubProcess() throws IOException {
        String sql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.57.0.sql"));

        assertThat(sql).contains(
                "ALTER TABLE `t_bpm_time_event`",
                "ADD COLUMN `graph_definition_version_id`",
                "ALTER TABLE `t_bpm_external_wait`",
                "CREATE TABLE IF NOT EXISTS `t_bpm_sub_process_link`",
                "UNIQUE KEY `uk_bpm_sub_process_event` (`event_key`)",
                "`called_definition_version_id` bigint NOT NULL",
                "'SUB_PROCESS'"
        );
    }

    @Test
    void definesM1GraphDraftAndTemplateStorageWithRevisionGuard() throws IOException {
        String sql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.50.0.sql"));
        String draftDao = Files.readString(Path.of("src/main/java/com/hunyuan/sa/bpm/module/model/dao/BpmProcessDraftDao.java"));

        assertThat(sql).contains("CREATE TABLE `t_bpm_process_draft`");
        assertThat(sql).contains("`revision` int NOT NULL");
        assertThat(sql).contains("`graph_json` longtext NOT NULL");
        assertThat(sql).contains("`layout_json` longtext NOT NULL");
        assertThat(sql).contains("`semantic_hash` char(64) NOT NULL");
        assertThat(sql).contains("CREATE TABLE `t_bpm_process_template`");
        assertThat(draftDao).contains("WHERE draft_id = #{draftId} AND revision = #{expectedRevision}");
        assertThat(sql).contains("bpm:graph-draft:add", "bpm:graph-draft:update", "bpm:graph-draft:detail");
        assertThat(sql).contains("bpm:graph-template:add", "bpm:graph-template:copy");
    }

    @Test
    void definesImmutableGraphDefinitionVersionAndElementMapping() throws IOException {
        String sql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.51.0.sql"));
        String permissionSql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.52.0.sql"));
        assertThat(sql).contains("CREATE TABLE `t_bpm_graph_definition_version`");
        assertThat(sql).contains("UNIQUE KEY `uk_bpm_graph_definition_version`");
        assertThat(sql).contains("`compiled_bpmn_xml` longtext NOT NULL", "`dependency_versions_json` longtext NOT NULL");
        assertThat(sql).contains("CREATE TABLE `t_bpm_graph_definition_mapping`");
        assertThat(sql).contains("UNIQUE KEY `uk_bpm_graph_mapping_authored`");
        assertThat(permissionSql).contains("bpm:graph-definition:publish", "bpm:graph-definition:deactivate");
    }

    @Test
    void definesVersionedM2CandidatePolicyAndM3BusinessContractCatalogs() throws IOException {
        String sql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.52.0.sql"));

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `t_bpm_candidate_policy_version`");
        assertThat(sql).contains("UNIQUE KEY `uk_bpm_candidate_policy_version`");
        assertThat(sql).contains("`policy_json` longtext NOT NULL");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `t_bpm_business_contract_version`");
        assertThat(sql).contains("UNIQUE KEY `uk_bpm_business_contract_version`");
        assertThat(sql).contains("`contract_json` longtext NOT NULL");
        assertThat(sql).contains("DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
    }

    @Test
    void definesGraphDefinitionInspectionPermissionInAnIncrementalMigration() throws IOException {
        String sql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.53.0.sql"));

        assertThat(sql).contains("bpm:graph-definition:detail");
        assertThat(sql).contains("(341, '查看Graph定义'");
    }

    @Test
    void definesM2ApprovalStageEngineBindingAndMemberTaskProjectionConstraints() throws IOException {
        String sql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.54.0.sql"));
        String originalTaskSql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.34.0.sql"));

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `t_bpm_approval_stage`");
        assertThat(sql).contains("`engine_process_instance_id` varchar(128) NOT NULL");
        assertThat(sql).contains("`engine_execution_id` varchar(128) NOT NULL");
        assertThat(sql).contains("`terminal_reason` varchar(128) NULL");
        assertThat(sql).contains("`engine_effect_state` varchar(32) NOT NULL");
        assertThat(sql).contains("ALTER TABLE `t_bpm_task`");
        assertThat(sql).contains("MODIFY COLUMN `engine_task_id` varchar(128) NULL");
        assertThat(sql).contains("`approval_stage_id` bigint NULL");
        assertThat(sql).contains("`approval_stage_member_id` bigint NULL");
        assertThat(sql).contains("UNIQUE KEY `uk_bpm_task_approval_stage_member` (`approval_stage_member_id`)");
        assertThat(sql).contains("KEY `idx_bpm_task_approval_stage_state` (`approval_stage_id`, `task_state`)");
        assertThat(sql).contains("ALTER TABLE `t_bpm_task_action_log`");
        assertThat(sql).contains("MODIFY COLUMN `definition_id` bigint NULL");
        assertThat(sql).contains("`graph_definition_version_id` bigint NULL");
        assertThat(sql).contains("KEY `idx_bpm_task_action_log_graph_definition` (`graph_definition_version_id`)");
        assertThat(originalTaskSql).contains("UNIQUE KEY `uk_engine_task` (`engine_task_id`)");
        assertThat(sql).doesNotContain("DROP INDEX `uk_engine_task`");
    }

    @Test
    void definesM2OrganizationRelationsAndCommandReceiptIdempotency() throws IOException {
        String sql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.54.0.sql"));

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `t_user_group`");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `t_user_group_employee`");
        assertThat(sql).contains("UNIQUE KEY `uk_user_group_employee` (`user_group_id`, `employee_id`)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `t_employee_reporting_relation`");
        assertThat(sql).contains("PRIMARY KEY (`employee_id`)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `t_bpm_approval_command_receipt`");
        assertThat(sql).contains("UNIQUE KEY `uk_bpm_approval_command_request` (`tenant_id`, `instance_id`, `request_id`)");
        assertThat(sql).contains("`command_fingerprint` char(64) NOT NULL");
        assertThat(sql).contains("`receipt_state` varchar(16) NOT NULL");
    }

    @Test
    void definesM3ApprovalSubjectAndSeparatedRuntimeDataPlanes() throws IOException {
        String sql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.55.0.sql"));

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `t_bpm_approval_subject_snapshot`");
        assertThat(sql).contains("UNIQUE KEY `uk_bpm_approval_subject_business_identity`");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `t_bpm_routing_fact_snapshot`");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `t_bpm_process_working_data`");
        assertThat(sql).contains("UNIQUE KEY `uk_bpm_working_data_subject_version`");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS `t_bpm_task_action_evidence`");
        assertThat(sql).contains("`before_working_data_version` bigint NOT NULL");
        assertThat(sql).contains("`after_working_data_version` bigint NOT NULL");
        assertThat(sql).contains("ALTER TABLE `t_bpm_instance`");
        assertThat(sql).contains("`approval_subject_snapshot_id` bigint NULL");
        assertThat(sql).contains("`routing_fact_snapshot_id` bigint NULL");
        assertThat(sql).contains("`process_working_data_id` bigint NULL");
        assertThat(sql).contains("UNIQUE KEY `uk_bpm_instance_approval_subject` (`approval_subject_snapshot_id`)");
        assertThat(sql).contains("ALTER TABLE `t_bpm_business_contract_version`");
        assertThat(sql).contains("information_schema.columns", "m3_contract_columns_sql");
        assertThat(sql).contains("m3_instance_columns_sql");
        assertThat(sql).contains("`contract_digest` char(64) NULL");
        assertThat(sql).contains("`catalog_revision` bigint NOT NULL DEFAULT 0");
        assertThat(sql).contains("BPM_BUSINESS_CONTRACT_LIFECYCLE");
        assertThat(sql).contains("BPM_DATA_SENSITIVITY");
        assertThat(sql).contains("BPM_DATA_CHANGE_POLICY");
        assertThat(sql).contains("/system/bpm/business-contract/business-contract-catalog");
        assertThat(sql).contains("bpm:business-contract:list", "bpm:business-contract:activate");
        assertThat(sql).contains("/system/bpm/runtime/generic-application");
    }

    @Test
    void definesM2ReleaseAndIneligibleMemberDictionaryValues() throws IOException {
        String sql = Files.readString(Path.of("../../数据库SQL脚本/mysql/sql-update-log/v3.54.0.sql"));

        assertThat(sql).contains("BPM_MODULE_RELEASE_STATUS");
        assertThat(sql).contains("'RELEASABLE'", "'NOT_RELEASABLE'");
        assertThat(sql).contains("'BPM_APPROVAL_STAGE_MEMBER_STATE', 'INELIGIBLE', '成员失效'");
    }
}
