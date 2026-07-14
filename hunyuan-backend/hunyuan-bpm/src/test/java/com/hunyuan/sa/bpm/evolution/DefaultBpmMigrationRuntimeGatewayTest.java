package com.hunyuan.sa.bpm.evolution;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmProcessWorkingDataDao;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionElementMappingDao;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionElementMappingEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.evolution.service.DefaultBpmMigrationRuntimeGateway;
import com.hunyuan.sa.bpm.module.evolution.service.MigrationDataMappingService;
import com.hunyuan.sa.bpm.module.evolution.service.MigrationSafetyEvaluator;
import com.hunyuan.sa.bpm.module.evolution.service.MigrationVariableEvidenceService;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmExternalWaitDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmSubProcessLinkDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTimeEventDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmExternalWaitEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmSubProcessLinkEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTimeEventEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class DefaultBpmMigrationRuntimeGatewayTest {

    @Test
    void unresolvedRuntimeFactsMustBeCollectedAndFailClosed() {
        DefaultBpmMigrationRuntimeGateway gateway = new DefaultBpmMigrationRuntimeGateway();
        GraphDefinitionElementMappingDao mappingDao = Mockito.mock(GraphDefinitionElementMappingDao.class);
        BpmTimeEventDao timerDao = Mockito.mock(BpmTimeEventDao.class);
        BpmExternalWaitDao externalWaitDao = Mockito.mock(BpmExternalWaitDao.class);
        BpmSubProcessLinkDao subProcessDao = Mockito.mock(BpmSubProcessLinkDao.class);
        BpmCallbackRecordDao callbackDao = Mockito.mock(BpmCallbackRecordDao.class);
        BpmTaskDao taskDao = Mockito.mock(BpmTaskDao.class);
        FlowableProcessInstanceGateway engine = Mockito.mock(FlowableProcessInstanceGateway.class);
        GraphDefinitionVersionDao versionDao = Mockito.mock(GraphDefinitionVersionDao.class);
        BpmProcessWorkingDataDao workingDataDao = Mockito.mock(BpmProcessWorkingDataDao.class);
        inject(gateway, "mappingDao", mappingDao);
        inject(gateway, "timeEventDao", timerDao);
        inject(gateway, "externalWaitDao", externalWaitDao);
        inject(gateway, "subProcessLinkDao", subProcessDao);
        inject(gateway, "callbackRecordDao", callbackDao);
        inject(gateway, "taskDao", taskDao);
        inject(gateway, "engineGateway", engine);
        inject(gateway, "safetyEvaluator", new MigrationSafetyEvaluator());
        inject(gateway, "versionDao", versionDao);
        inject(gateway, "dataMappingService", new MigrationDataMappingService());
        inject(gateway, "variableEvidenceService", new MigrationVariableEvidenceService());
        inject(gateway, "workingDataDao", workingDataDao);

        GraphDefinitionVersionEntity source = version(1L, "engine-v1");
        GraphDefinitionVersionEntity target = version(2L, "engine-v2");
        Mockito.when(versionDao.selectById(1L)).thenReturn(source);
        Mockito.when(mappingDao.selectList(any())).thenReturn(
                List.of(mapping(1L, "wait", "compiled-v1")),
                List.of(mapping(2L, "wait", "compiled-v2")));
        Mockito.when(taskDao.selectList(any())).thenReturn(List.of(new BpmTaskEntity()));
        Mockito.when(timerDao.selectList(any())).thenReturn(List.of(new BpmTimeEventEntity()));
        Mockito.when(externalWaitDao.selectList(any())).thenReturn(List.of(new BpmExternalWaitEntity()));
        Mockito.when(subProcessDao.selectList(any())).thenReturn(List.of(new BpmSubProcessLinkEntity()));
        Mockito.when(callbackDao.selectList(any())).thenReturn(List.of(new BpmCallbackRecordEntity()));
        Mockito.when(engine.activeActivityIds("pi-11")).thenReturn(Set.of("compiled-v1"));
        Mockito.when(engine.activeActivityCount("pi-11")).thenReturn(2L);
        Mockito.when(engine.activeTaskCount("pi-11")).thenReturn(0L);
        Mockito.when(engine.activeChildProcessInstanceIds("pi-11")).thenReturn(List.of("child-1"));
        Mockito.when(engine.variables("pi-11")).thenReturn(Map.of());

        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(11L); instance.setGraphDefinitionVersionId(1L);
        instance.setEngineProcessInstanceId("pi-11"); instance.setRunState(1);
        instance.setCurrentFormDataSnapshotJson("{}");
        var result = gateway.assess(instance, target, Map.of("wait", "wait"), "{}");

        assertThat(result.blockers()).extracting(blocker -> blocker.code())
                .contains("ACTIVE_HUMAN_TASK", "ACTIVE_PARALLEL_PATH", "PENDING_TIMER",
                        "EXTERNAL_WAIT", "ACTIVE_SUB_PROCESS", "IRREVERSIBLE_SIDE_EFFECT");
        assertQueryValues(timerDao, "FAILED_MANUAL");
        assertQueryValues(subProcessDao, "FAILED_MANUAL", "FAILED_PAUSED");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void assertQueryValues(Object dao, Object... expected) {
        Class<?> entityType = dao instanceof BpmTimeEventDao ? BpmTimeEventEntity.class : BpmSubProcessLinkEntity.class;
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "m8-test"), entityType);
        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        if (dao instanceof BpmTimeEventDao timerDao) Mockito.verify(timerDao).selectList(captor.capture());
        if (dao instanceof BpmSubProcessLinkDao subProcessDao) Mockito.verify(subProcessDao).selectList(captor.capture());
        AbstractWrapper wrapper = (AbstractWrapper) captor.getValue();
        assertThat(wrapper.getSqlSegment()).isNotBlank();
        Map<String, Object> values = wrapper.getParamNameValuePairs();
        assertThat(values.values()).contains(expected);
    }

    private GraphDefinitionVersionEntity version(Long id, String engineId) {
        GraphDefinitionVersionEntity version = new GraphDefinitionVersionEntity();
        version.setGraphDefinitionVersionId(id); version.setEngineProcessDefinitionId(engineId);
        version.setDependencyVersionsJson("{\"businessContract\":{\"canonicalPayload\":"
                + "\"{\\\"fieldSchema\\\":[],\\\"workingDataSchema\\\":[]}\"}}");
        return version;
    }

    private GraphDefinitionElementMappingEntity mapping(Long versionId, String authoredId, String compiledId) {
        GraphDefinitionElementMappingEntity mapping = new GraphDefinitionElementMappingEntity();
        mapping.setGraphDefinitionVersionId(versionId); mapping.setAuthoredElementId(authoredId);
        mapping.setCompiledElementId(compiledId);
        return mapping;
    }

    private void inject(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
