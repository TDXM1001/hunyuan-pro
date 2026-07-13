package com.hunyuan.sa.bpm.integration;

import com.hunyuan.sa.bpm.module.integration.domain.command.BpmExternalStartCommand;
import com.hunyuan.sa.bpm.module.integration.service.BpmExternalProcessService;
import com.hunyuan.sa.bpm.module.integration.service.BpmExternalStartCommandStore;
import com.hunyuan.sa.bpm.module.integration.service.BpmExternalPublicReferenceService;
import com.hunyuan.sa.bpm.module.integration.service.BpmExternalEmployeeMappingService;
import com.hunyuan.sa.bpm.module.integration.service.BpmProcessBindingService;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCommandRecordEntity;
import com.hunyuan.sa.bpm.module.integration.domain.model.BpmSourceApplicationPrincipal;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmGenericApplicationService;
import com.hunyuan.sa.bpm.module.businesscontract.service.BpmBusinessContractCatalogService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class BpmM6OpenProtocolContractTest {

    @Test
    void externalApprovalSubjectMustNotExposeTheInternalSnapshotId() {
        assertThat(java.util.Arrays.stream(com.hunyuan.sa.bpm.module.integration.domain.model.BpmExternalTaskView.ApprovalSubjectView.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)).doesNotContain("approvalSubjectSnapshotId");
    }

    @Test
    void duplicateStartRequestMustReturnTheSamePublicInstanceId() {
        BpmExternalStartCommandStore commandStore = Mockito.mock(BpmExternalStartCommandStore.class);
        BpmExternalPublicReferenceService publicReferences=Mockito.mock(BpmExternalPublicReferenceService.class);
        Mockito.when(publicReferences.getOrCreate("PURCHASE","INSTANCE",88L)).thenReturn("BP-opaque-88");
        BpmExternalProcessService service = new BpmExternalProcessService(commandStore,
                Mockito.mock(BpmExternalEmployeeMappingService.class), Mockito.mock(BpmProcessBindingService.class),
                Mockito.mock(BpmGenericApplicationService.class), Mockito.mock(BpmBusinessContractCatalogService.class), Mockito.mock(BpmTaskService.class),publicReferences,Mockito.mock(com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao.class));
        BpmExternalStartCommand command = new BpmExternalStartCommand(
                "request-1", "purchase-contract", 2, "purchase", "PO-1001", "DEFAULT", "buyer-100",
                "采购申请", "{\"amount\":5000}"
        );
        BpmCommandRecordEntity existing = new BpmCommandRecordEntity();
        existing.setCommandStatus(1); existing.setInstanceId(88L);
        existing.setRequestPayloadJson(com.alibaba.fastjson.JSON.toJSONString(command));
        Mockito.when(commandStore.loadOrCreate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new BpmExternalStartCommandStore.Claim(existing, false));
        BpmSourceApplicationPrincipal principal = new BpmSourceApplicationPrincipal(1L,"PURCHASE","purchase-app","process:start","ACTIVE");

        assertThat(service.start(principal, command)).isEqualTo("BP-opaque-88");
        assertThat(service.start(principal, command)).isEqualTo("BP-opaque-88");
        Mockito.verify(commandStore, Mockito.never()).markSucceeded(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    void failedStartMustPersistTheFailureOutsideTheBusinessTransaction() {
        BpmExternalStartCommandStore commandStore = Mockito.mock(BpmExternalStartCommandStore.class);
        BpmExternalEmployeeMappingService mappings = Mockito.mock(BpmExternalEmployeeMappingService.class);
        BpmProcessBindingService bindings = Mockito.mock(BpmProcessBindingService.class);
        BpmExternalProcessService service = new BpmExternalProcessService(commandStore, mappings, bindings,
                Mockito.mock(BpmGenericApplicationService.class), Mockito.mock(BpmBusinessContractCatalogService.class), Mockito.mock(BpmTaskService.class),Mockito.mock(BpmExternalPublicReferenceService.class),Mockito.mock(com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao.class));
        BpmExternalStartCommand command = new BpmExternalStartCommand(
                "request-2", "purchase-contract", 2, "purchase", "PO-1002", "DEFAULT", "buyer-100",
                "采购申请", "{\"amount\":5000}"
        );
        BpmCommandRecordEntity pending = new BpmCommandRecordEntity();
        pending.setCommandRecordId(7L); pending.setCommandStatus(0);
        pending.setRequestPayloadJson(com.alibaba.fastjson.JSON.toJSONString(command));
        Mockito.when(commandStore.loadOrCreate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new BpmExternalStartCommandStore.Claim(pending, true));
        Mockito.when(mappings.requireEmployee(Mockito.any(), Mockito.anyString())).thenThrow(new IllegalStateException("员工映射不存在"));
        BpmSourceApplicationPrincipal principal = new BpmSourceApplicationPrincipal(1L,"PURCHASE","purchase-app","process:start","ACTIVE");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.start(principal, command))
                .isInstanceOf(IllegalStateException.class);
        Mockito.verify(commandStore).markFailed(7L, "员工映射不存在");
    }

    @Test
    void concurrentPendingStartMustNotEnterTheBusinessSubmissionAgain() {
        BpmExternalStartCommandStore commandStore = Mockito.mock(BpmExternalStartCommandStore.class);
        BpmGenericApplicationService applications = Mockito.mock(BpmGenericApplicationService.class);
        BpmExternalProcessService service = new BpmExternalProcessService(commandStore,
                Mockito.mock(BpmExternalEmployeeMappingService.class), Mockito.mock(BpmProcessBindingService.class),
                applications, Mockito.mock(BpmBusinessContractCatalogService.class), Mockito.mock(BpmTaskService.class),Mockito.mock(BpmExternalPublicReferenceService.class),Mockito.mock(com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao.class));
        BpmCommandRecordEntity pending = new BpmCommandRecordEntity();
        pending.setCommandRecordId(8L); pending.setCommandStatus(0);
        BpmExternalStartCommand command = new BpmExternalStartCommand(
                "request-3", "purchase-contract", 2, "purchase", "PO-1003", "DEFAULT", "buyer-100",
                "采购申请", "{\"amount\":5000}"
        );
        pending.setRequestPayloadJson(com.alibaba.fastjson.JSON.toJSONString(command));
        Mockito.when(commandStore.loadOrCreate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new BpmExternalStartCommandStore.Claim(pending, false));
        BpmSourceApplicationPrincipal principal = new BpmSourceApplicationPrincipal(1L,"PURCHASE","purchase-app","process:start","ACTIVE");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.start(principal, command))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("正在处理");
        Mockito.verifyNoInteractions(applications);
    }
}
