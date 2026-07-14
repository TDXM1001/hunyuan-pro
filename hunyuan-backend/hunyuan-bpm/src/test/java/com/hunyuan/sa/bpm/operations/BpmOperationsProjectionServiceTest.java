package com.hunyuan.sa.bpm.operations;

import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCommandRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCommandRecordEntity;
import com.hunyuan.sa.bpm.module.operations.dao.BpmOperationsCaseDao;
import com.hunyuan.sa.bpm.module.operations.dao.BpmOperationsRetentionPolicyDao;
import com.hunyuan.sa.bpm.module.operations.domain.entity.BpmOperationsCaseEntity;
import com.hunyuan.sa.bpm.module.operations.service.BpmOperationsProjectionService;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmExternalWaitDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmNotificationRecordDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTimeEventDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmExternalWaitEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmNotificationRecordEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTimeEventEntity;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class BpmOperationsProjectionServiceTest {

    @Test
    void refreshMustDetectRuntimeAndIntegrationFailuresIntoUnifiedQueue() {
        BpmOperationsCaseDao caseDao = Mockito.mock(BpmOperationsCaseDao.class);
        BpmTimeEventDao timeEventDao = Mockito.mock(BpmTimeEventDao.class);
        BpmExternalWaitDao externalWaitDao = Mockito.mock(BpmExternalWaitDao.class);
        BpmCallbackRecordDao callbackDao = Mockito.mock(BpmCallbackRecordDao.class);
        BpmCommandRecordDao commandDao = Mockito.mock(BpmCommandRecordDao.class);
        BpmNotificationRecordDao notificationDao = Mockito.mock(BpmNotificationRecordDao.class);
        BpmInstanceDao instanceDao = Mockito.mock(BpmInstanceDao.class);
        BpmOperationsRetentionPolicyDao retentionPolicyDao = Mockito.mock(BpmOperationsRetentionPolicyDao.class);
        BpmOrgIdentityGateway orgIdentityGateway = Mockito.mock(BpmOrgIdentityGateway.class);

        BpmTimeEventEntity timeEvent = new BpmTimeEventEntity();
        timeEvent.setTimeEventId(1L);
        timeEvent.setInstanceId(100L);
        timeEvent.setEventStatus("FAILED_MANUAL");
        timeEvent.setEventKind("SLA_DEADLINE");
        timeEvent.setLastError("timer failed");
        BpmExternalWaitEntity externalWait = new BpmExternalWaitEntity();
        externalWait.setExternalWaitId(2L);
        externalWait.setInstanceId(100L);
        externalWait.setWaitStatus("TIMED_OUT");
        externalWait.setLastError("wait timeout");
        BpmCallbackRecordEntity callback = new BpmCallbackRecordEntity();
        callback.setCallbackRecordId(3L);
        callback.setInstanceId(100L);
        callback.setCallbackStatus(3);
        callback.setEventId("evt-3");
        callback.setFailureReason("callback exhausted");
        BpmCommandRecordEntity command = new BpmCommandRecordEntity();
        command.setCommandRecordId(4L);
        command.setInstanceId(100L);
        command.setCommandStatus(2);
        command.setFailureReason("command failed");
        BpmNotificationRecordEntity notification = new BpmNotificationRecordEntity();
        notification.setNotificationRecordId(5L);
        notification.setInstanceId(100L);
        notification.setSendStatus(2);
        notification.setFailReason("notification failed");

        Mockito.when(timeEventDao.selectList(any())).thenReturn(List.of(timeEvent));
        Mockito.when(externalWaitDao.selectList(any())).thenReturn(List.of(externalWait));
        Mockito.when(callbackDao.selectList(any())).thenReturn(List.of(callback));
        Mockito.when(commandDao.selectList(any())).thenReturn(List.of(command));
        Mockito.when(notificationDao.selectList(any())).thenReturn(List.of(notification));
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(100L);
        instance.setGraphDefinitionVersionId(88L);
        instance.setStartDepartmentIdSnapshot(9L);
        instance.setBusinessKey("EXP-100");
        Mockito.when(instanceDao.selectBatchIds(any())).thenReturn(List.of(instance));
        Mockito.when(orgIdentityGateway.resolveDepartmentManagerEmployeeId(9L)).thenReturn(901L);
        Mockito.when(caseDao.selectOne(any())).thenReturn(null);

        BpmOperationsProjectionService service = new BpmOperationsProjectionService(
                caseDao, timeEventDao, externalWaitDao, callbackDao, commandDao, notificationDao,
                instanceDao, retentionPolicyDao, orgIdentityGateway
        );

        int detected = service.refresh();

        assertThat(detected).isEqualTo(5);
        ArgumentCaptor<BpmOperationsCaseEntity> captor = ArgumentCaptor.forClass(BpmOperationsCaseEntity.class);
        Mockito.verify(caseDao, Mockito.times(5)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(BpmOperationsCaseEntity::getSourceType)
                .containsExactlyInAnyOrder("TIME_EVENT", "EXTERNAL_WAIT", "CALLBACK", "COMMAND", "NOTIFICATION");
        assertThat(captor.getAllValues()).allSatisfy(item -> {
            assertThat(item.getGraphDefinitionVersionId()).isEqualTo(88L);
            assertThat(item.getOrganizationId()).isEqualTo(9L);
            assertThat(item.getAssigneeEmployeeId()).isEqualTo(901L);
            assertThat(item.getBusinessKey()).isEqualTo("EXP-100");
            assertThat(item.getCaseStatus()).isEqualTo("OPEN");
        });
    }
}
