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
        assertThat(sql).contains("BPM_COMMAND_TYPE", "BPM_COMMAND_STATUS", "BPM_SAMPLE_EXPENSE_APPROVAL_STATUS");
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
}
