package com.hunyuan.sa.bpm.schema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BpmSchemaSourceTest {

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
