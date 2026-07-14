package com.hunyuan.sa.admin.module.bpm;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.alibaba.fastjson.JSON;
import com.hunyuan.sa.admin.module.bpm.adapter.AdminBpmCurrentActorProvider;
import com.hunyuan.sa.base.handler.MybatisPlusFillHandler;
import com.hunyuan.sa.bpm.api.identity.BpmActorScope;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.config.BpmFlowableAutoConfiguration;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionElementMappingDao;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionElementMappingEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.evolution.dao.BpmMigrationBatchDao;
import com.hunyuan.sa.bpm.module.evolution.dao.BpmMigrationItemDao;
import com.hunyuan.sa.bpm.module.evolution.domain.entity.BpmMigrationItemEntity;
import com.hunyuan.sa.bpm.module.evolution.domain.entity.BpmMigrationBatchEntity;
import com.hunyuan.sa.bpm.module.evolution.domain.form.BpmMigrationPreviewForm;
import com.hunyuan.sa.bpm.module.evolution.service.BpmMigrationItemExecutor;
import com.hunyuan.sa.bpm.module.evolution.service.BpmInstanceMigrationService;
import com.hunyuan.sa.bpm.module.evolution.service.DefaultBpmMigrationRuntimeGateway;
import com.hunyuan.sa.bpm.module.evolution.service.MigrationDataMappingService;
import com.hunyuan.sa.bpm.module.evolution.service.MigrationSafetyEvaluator;
import com.hunyuan.sa.bpm.module.evolution.service.MigrationVariableEvidenceService;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import jakarta.annotation.Resource;
import org.apache.ibatis.annotations.Mapper;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = BpmM8LiveMigrationAcceptanceTest.M8LiveTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/hunyuan?useServerPreparedStmts=false&rewriteBatchedStatements=true&characterEncoding=UTF-8&useSSL=false&allowMultiQueries=true&serverTimezone=Asia/Shanghai",
                "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                "spring.datasource.username=root",
                "spring.datasource.password=root",
                "spring.datasource.druid.username=root",
                "spring.datasource.druid.password=root",
                "bpm.flowable.enabled=true",
                "bpm.flowable.database-schema-update=false",
                "bpm.flowable.async-executor-activate=false",
                "knife4j.enable=false",
                "spring.cache.type=simple",
                "spring.data.redis.repositories.enabled=false"
        })
@EnabledIfSystemProperty(named = "m8.live", matches = "true")
class BpmM8LiveMigrationAcceptanceTest {
    @MockBean private BpmOrgIdentityGateway orgIdentityGateway;
    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.redisson.spring.starter.RedissonAutoConfigurationV2",
            "cn.dev33.satoken.spring.SaBeanRegister",
            "org.flowable.spring.boot.FlowableJpaAutoConfiguration",
            "org.flowable.spring.boot.EndpointAutoConfiguration",
            "org.flowable.spring.boot.RestApiAutoConfiguration",
            "org.flowable.spring.boot.FlowableSecurityAutoConfiguration",
            "org.flowable.spring.boot.actuate.info.FlowableInfoAutoConfiguration",
            "org.flowable.spring.boot.ldap.FlowableLdapAutoConfiguration",
            "org.flowable.spring.boot.app.AppEngineAutoConfiguration",
            "org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration",
            "org.flowable.spring.boot.dmn.DmnEngineAutoConfiguration",
            "org.flowable.spring.boot.dmn.DmnEngineServicesAutoConfiguration",
            "org.flowable.spring.boot.cmmn.CmmnEngineAutoConfiguration",
            "org.flowable.spring.boot.cmmn.CmmnEngineServicesAutoConfiguration",
            "org.flowable.spring.boot.idm.IdmEngineAutoConfiguration",
            "org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration",
            "org.flowable.spring.boot.eventregistry.EventRegistryAutoConfiguration",
            "org.flowable.spring.boot.eventregistry.EventRegistryServicesAutoConfiguration"
    })
    @MapperScan(value = "com.hunyuan.sa.bpm", annotationClass = Mapper.class)
    @Import({
            BpmFlowableAutoConfiguration.class,
            FlowableProcessInstanceGateway.class,
            AdminBpmCurrentActorProvider.class,
            BpmInstanceMigrationService.class,
            BpmMigrationItemExecutor.class,
            DefaultBpmMigrationRuntimeGateway.class,
            MigrationDataMappingService.class,
            MigrationVariableEvidenceService.class,
            MigrationSafetyEvaluator.class,
            MybatisPlusFillHandler.class
    })
    static class M8LiveTestApplication {
    }

    @Resource private RepositoryService repositoryService;
    @Resource private RuntimeService runtimeService;
    @Resource private HistoryService historyService;
    @Resource private GraphDefinitionVersionDao versionDao;
    @Resource private GraphDefinitionElementMappingDao mappingDao;
    @Resource private BpmInstanceDao instanceDao;
    @Resource private BpmMigrationBatchDao batchDao;
    @Resource private BpmMigrationItemDao itemDao;
    @Resource private BpmInstanceMigrationService migrationService;
    @Resource private PlatformTransactionManager transactionManager;

    @Test
    void eligibleIdleInstanceMustMigrateAndContinueOnTargetDefinition() {
        String key = "m8_live_" + System.currentTimeMillis();
        Deployment sourceDeployment = null;
        Deployment targetDeployment = null;
        GraphDefinitionVersionEntity sourceVersion = null;
        GraphDefinitionVersionEntity targetVersion = null;
        BpmInstanceEntity instance = null;
        Long batchId = null;
        try {
            sourceDeployment = repositoryService.createDeployment().name(key + "-v1")
                    .addString(key + "-v1.bpmn20.xml", bpmn(key, "wait_v1")).deploy();
            ProcessDefinition sourceDefinition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(sourceDeployment.getId()).singleResult();
            targetDeployment = repositoryService.createDeployment().name(key + "-v2")
                    .addString(key + "-v2.bpmn20.xml", bpmn(key, "wait_v2")).deploy();
            ProcessDefinition targetDefinition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(targetDeployment.getId()).singleResult();

            sourceVersion = version(key, 1, sourceDeployment, sourceDefinition, "wait_v1");
            targetVersion = version(key, 2, targetDeployment, targetDefinition, "wait_v2");
            versionDao.insert(sourceVersion); versionDao.insert(targetVersion);
            mapping(sourceVersion.getGraphDefinitionVersionId(), "wait_v1");
            mapping(targetVersion.getGraphDefinitionVersionId(), "wait_v2");

            String sourceForm = "{\"a\":\"A\",\"b\":\"B\"}";
            String engineInstanceId = runtimeService.startProcessInstanceById(sourceDefinition.getId(), Map.of(
                    "legacySecret", "sensitive-value",
                    "formDataJson", sourceForm,
                    "formData", JSON.parseObject(sourceForm))).getId();
            instance = instance(key, sourceVersion, sourceDefinition, engineInstanceId);
            instance.setInitialFormDataSnapshotJson(sourceForm);
            instance.setCurrentFormDataSnapshotJson(sourceForm);
            instanceDao.insert(instance);

            BpmMigrationPreviewForm preview = new BpmMigrationPreviewForm();
            preview.setSourceVersionId(sourceVersion.getGraphDefinitionVersionId());
            preview.setTargetVersionId(targetVersion.getGraphDefinitionVersionId());
            preview.setInstanceIds(List.of(instance.getInstanceId()));
            preview.setNodeMappings(Map.of("wait", "wait"));
            preview.setDataMappingJson("{\"fieldMappings\":{\"a\":\"b\",\"b\":\"c\"},"
                    + "\"variableMappings\":{\"legacySecret\":\"currentSecret\"}}");
            preview.setIdempotencyKey(key);
            preview.setReason("M8 实库正向迁移验收");
            var previewResult = BpmActorScope.runAs(1L, () -> migrationService.preview(preview));
            assertThat(previewResult.getOk()).isTrue();
            assertThat(previewResult.getData().getEligibleCount()).isEqualTo(1);
            batchId = previewResult.getData().getMigrationBatchId();

            Long finalBatchId = batchId;
            var executeResult = BpmActorScope.runAs(1L, () -> migrationService.execute(finalBatchId));
            assertThat(executeResult.getOk()).isTrue();
            assertThat(executeResult.getData().getSucceededCount()).isEqualTo(1);
            assertThat(executeResult.getData().getItems().get(0).getEngineCommandEvidenceJson())
                    .contains(targetDefinition.getId(), "MIGRATED", "legacySecret", "currentSecret",
                            "sourceVariablesDigest", "targetVariablesDigest")
                    .doesNotContain("sensitive-value");

            BpmInstanceEntity migrated = instanceDao.selectById(instance.getInstanceId());
            assertThat(migrated.getGraphDefinitionVersionId()).isEqualTo(targetVersion.getGraphDefinitionVersionId());
            assertThat(JSON.parseObject(migrated.getCurrentFormDataSnapshotJson()))
                    .containsEntry("b", "A").containsEntry("c", "B").doesNotContainKey("a");
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(engineInstanceId)
                    .singleResult().getProcessDefinitionId()).isEqualTo(targetDefinition.getId());
            assertThat(JSON.parseObject((String) runtimeService.getVariable(engineInstanceId, "formDataJson")))
                    .containsEntry("b", "A").containsEntry("c", "B").doesNotContainKey("a");
            assertThat(runtimeService.getVariable(engineInstanceId, "currentSecret")).isEqualTo("sensitive-value");

            String executionId = runtimeService.createExecutionQuery().processInstanceId(engineInstanceId)
                    .activityId("wait_v2").singleResult().getId();
            runtimeService.trigger(executionId);
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(engineInstanceId)
                    .finished().singleResult()).isNotNull();
        } finally {
            if (batchId != null) {
                itemDao.delete(Wrappers.<BpmMigrationItemEntity>lambdaQuery()
                        .eq(BpmMigrationItemEntity::getMigrationBatchId, batchId));
                batchDao.deleteById(batchId);
            }
            if (instance != null && instance.getInstanceId() != null) instanceDao.deleteById(instance.getInstanceId());
            if (sourceVersion != null && sourceVersion.getGraphDefinitionVersionId() != null) {
                mappingDao.delete(Wrappers.<GraphDefinitionElementMappingEntity>lambdaQuery()
                        .eq(GraphDefinitionElementMappingEntity::getGraphDefinitionVersionId,
                                sourceVersion.getGraphDefinitionVersionId()));
                versionDao.deleteById(sourceVersion.getGraphDefinitionVersionId());
            }
            if (targetVersion != null && targetVersion.getGraphDefinitionVersionId() != null) {
                mappingDao.delete(Wrappers.<GraphDefinitionElementMappingEntity>lambdaQuery()
                        .eq(GraphDefinitionElementMappingEntity::getGraphDefinitionVersionId,
                                targetVersion.getGraphDefinitionVersionId()));
                versionDao.deleteById(targetVersion.getGraphDefinitionVersionId());
            }
            if (targetDeployment != null) repositoryService.deleteDeployment(targetDeployment.getId(), true);
            if (sourceDeployment != null) repositoryService.deleteDeployment(sourceDeployment.getId(), true);
        }
        assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionKey(key).count()).isZero();
        assertThat(repositoryService.createDeploymentQuery().deploymentNameLike(key + "%").count()).isZero();
        assertThat(versionDao.selectCount(Wrappers.<GraphDefinitionVersionEntity>lambdaQuery()
                .eq(GraphDefinitionVersionEntity::getProcessKey, key))).isZero();
        assertThat(instanceDao.selectCount(Wrappers.<BpmInstanceEntity>lambdaQuery()
                .eq(BpmInstanceEntity::getDefinitionKeySnapshot, key))).isZero();
        assertThat(batchDao.selectByIdempotencyKey(key)).isNull();
        assertThat(itemDao.selectByBatchId(batchId)).isEmpty();
    }

    @Test
    void duplicateKeyRaceMustRecoverWithCurrentLockingRead() throws Exception {
        String key = "m8_race_" + System.currentTimeMillis();
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch competitorReady = new CountDownLatch(1);
        CountDownLatch releaseWinner = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var winner = pool.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                batchDao.insert(batch(key));
                inserted.countDown();
                await(releaseWinner);
                return null;
            }));
            var competitor = pool.submit(() -> {
                assertThat(inserted.await(10, TimeUnit.SECONDS)).isTrue();
                return new TransactionTemplate(transactionManager).execute(status -> {
                    assertThat(batchDao.selectByIdempotencyKey(key)).isNull();
                    competitorReady.countDown();
                    try {
                        batchDao.insert(batch(key));
                        throw new AssertionError("唯一键竞争必须失败");
                    } catch (DuplicateKeyException expected) {
                        return batchDao.selectByIdempotencyKeyForUpdate(key);
                    }
                });
            });
            assertThat(competitorReady.await(10, TimeUnit.SECONDS)).isTrue();
            releaseWinner.countDown();
            winner.get(10, TimeUnit.SECONDS);
            BpmMigrationBatchEntity observed = competitor.get(10, TimeUnit.SECONDS);
            assertThat(observed).isNotNull();
            assertThat(observed.getIdempotencyKey()).isEqualTo(key);
        } finally {
            releaseWinner.countDown();
            pool.shutdownNow();
            BpmMigrationBatchEntity residue = batchDao.selectByIdempotencyKey(key);
            if (residue != null) batchDao.deleteById(residue.getMigrationBatchId());
        }
        assertThat(batchDao.selectByIdempotencyKey(key)).isNull();
    }

    @Test
    void expiredLeaseMustBeTakenOverAndFenceStaleOwner() {
        String key = "m8_lease_" + System.currentTimeMillis();
        BpmMigrationBatchEntity batch = batch(key);
        batch.setBatchStatus("EXECUTING");
        batch.setExecutionOwnerKey("stale-owner");
        batch.setExecutionLeaseUntil(LocalDateTime.now().minusMinutes(1));
        batchDao.insert(batch);
        try {
            assertThat(batchDao.claimForExecution(batch.getMigrationBatchId(), "fresh-owner", 1L)).isEqualTo(1);
            assertThat(batchDao.renewExecutionLease(batch.getMigrationBatchId(), "stale-owner")).isZero();
            assertThat(batchDao.finalizeExecution(batch.getMigrationBatchId(), "stale-owner",
                    "SUCCEEDED", 0, 0)).isZero();
            assertThat(batchDao.renewExecutionLease(batch.getMigrationBatchId(), "fresh-owner")).isEqualTo(1);
            assertThat(batchDao.finalizeExecution(batch.getMigrationBatchId(), "fresh-owner",
                    "SUCCEEDED", 0, 0)).isEqualTo(1);
        } finally {
            batchDao.deleteById(batch.getMigrationBatchId());
        }
        assertThat(batchDao.selectByIdempotencyKey(key)).isNull();
    }

    private GraphDefinitionVersionEntity version(String key, int version, Deployment deployment,
                                                   ProcessDefinition definition, String waitId) {
        GraphDefinitionVersionEntity entity = new GraphDefinitionVersionEntity();
        entity.setDraftId(-System.nanoTime()); entity.setProcessKey(key); entity.setProcessNameSnapshot(key);
        entity.setDefinitionVersion(version); entity.setLifecycleState("ACTIVE");
        entity.setGraphSnapshotJson("{\"nodes\":[{\"nodeId\":\"wait\",\"type\":\"WAIT\",\"name\":\"wait\",\"properties\":{}}],\"edges\":[],\"scopes\":[]}");
        entity.setLayoutSnapshotJson("{}"); entity.setSemanticHash("0".repeat(64));
        String contract = version == 1
                ? "{\"fieldSchema\":[{\"key\":\"a\",\"type\":\"STRING\",\"required\":true},"
                    + "{\"key\":\"b\",\"type\":\"STRING\",\"required\":true}],\"workingDataSchema\":[]}"
                : "{\"fieldSchema\":[{\"key\":\"b\",\"type\":\"STRING\",\"required\":true},"
                    + "{\"key\":\"c\",\"type\":\"STRING\",\"required\":true}],\"workingDataSchema\":[]}";
        entity.setDependencyVersionsJson(JSON.toJSONString(Map.of("businessContract",
                Map.of("canonicalPayload", contract))));
        entity.setCompilerVersion("m8-live");
        entity.setCompiledBpmnXml(bpmn(key, waitId)); entity.setDeploymentId(deployment.getId());
        entity.setEngineProcessDefinitionId(definition.getId()); entity.setPublishedByEmployeeId(1L);
        entity.setCreateTime(LocalDateTime.now()); entity.setUpdateTime(LocalDateTime.now());
        return entity;
    }

    private void mapping(Long versionId, String compiledId) {
        GraphDefinitionElementMappingEntity mapping = new GraphDefinitionElementMappingEntity();
        mapping.setGraphDefinitionVersionId(versionId); mapping.setAuthoredElementId("wait");
        mapping.setAuthoredElementKind("WAIT"); mapping.setCompiledElementId(compiledId);
        mapping.setCompiledElementType("receiveTask"); mappingDao.insert(mapping);
    }

    private BpmInstanceEntity instance(String key, GraphDefinitionVersionEntity version,
                                       ProcessDefinition definition, String engineInstanceId) {
        LocalDateTime now = LocalDateTime.now();
        BpmInstanceEntity entity = new BpmInstanceEntity();
        entity.setInstanceNo("M8-" + System.nanoTime()); entity.setGraphDefinitionVersionId(version.getGraphDefinitionVersionId());
        entity.setDefinitionSource("GRAPH"); entity.setEngineProcessDefinitionId(definition.getId());
        entity.setEngineProcessInstanceId(engineInstanceId); entity.setDefinitionKeySnapshot(key);
        entity.setDefinitionVersionSnapshot(1); entity.setCategoryIdSnapshot(1L); entity.setCategoryNameSnapshot("M8");
        entity.setTitle("M8 live acceptance"); entity.setStartEmployeeId(1L); entity.setStartEmployeeNameSnapshot("admin");
        entity.setBusinessType("M8_ACCEPTANCE"); entity.setBusinessKey(key); entity.setInitialFormDataSnapshotJson("{}");
        entity.setCurrentFormDataSnapshotJson("{}"); entity.setFormDataVersion(1L); entity.setRunState(1);
        entity.setActiveTaskCount(0); entity.setCurrentGeneration(1); entity.setStartedAt(now);
        entity.setLastActionAt(now); entity.setCreateTime(now); entity.setUpdateTime(now);
        return entity;
    }

    private String bpmn(String key, String waitId) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" targetNamespace=\"m8\">"
                + "<process id=\"" + key + "\" name=\"M8 Live\" isExecutable=\"true\">"
                + "<startEvent id=\"start\"/><sequenceFlow id=\"f1\" sourceRef=\"start\" targetRef=\"" + waitId + "\"/>"
                + "<receiveTask id=\"" + waitId + "\" name=\"Migration checkpoint\"/>"
                + "<sequenceFlow id=\"f2\" sourceRef=\"" + waitId + "\" targetRef=\"end\"/><endEvent id=\"end\"/>"
                + "</process></definitions>";
    }

    private BpmMigrationBatchEntity batch(String key) {
        BpmMigrationBatchEntity entity = new BpmMigrationBatchEntity();
        entity.setBatchCode("MIG-" + key); entity.setIdempotencyKey(key);
        entity.setSourceVersionId(1L); entity.setTargetVersionId(2L); entity.setBatchStatus("PREVIEWED");
        entity.setMappingJson("{}"); entity.setDataMappingJson("{}"); entity.setReason("并发幂等实库验收");
        entity.setActorEmployeeId(1L); entity.setTotalCount(0); entity.setEligibleCount(0);
        entity.setBlockedCount(0); entity.setSucceededCount(0); entity.setFailedCount(0);
        entity.setPreviewedAt(LocalDateTime.now()); entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        return entity;
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("并发测试等待超时");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("并发测试被中断", ex);
        }
    }
}
