package com.hunyuan.sa.bpm.approvaldata;

import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmApprovalSubjectSnapshotDao;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmProcessWorkingDataDao;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmRoutingFactSnapshotDao;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmApprovalSubjectSnapshotEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmProcessWorkingDataEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmRoutingFactSnapshotEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.ApprovalRuntimeBinding;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmApprovalRuntimeDataService;
import com.hunyuan.sa.bpm.module.candidate.domain.model.RoutingFactView;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class BpmApprovalRuntimeDataServiceTest {

    @Test
    void routingDataShouldExposeOnlyFrozenAllowedFacts() {
        BpmRoutingFactSnapshotDao routingFactDao = Mockito.mock(BpmRoutingFactSnapshotDao.class);
        BpmRoutingFactSnapshotEntity routing = new BpmRoutingFactSnapshotEntity();
        routing.setRoutingFactSnapshotId(51L);
        routing.setRoutingFactVersion(7L);
        routing.setFactsJson("{\"amount\":9000,\"privateMemo\":\"hidden\"}");
        routing.setAllowedFactKeysJson("[\"amount\"]");
        when(routingFactDao.selectById(51L)).thenReturn(routing);
        BpmApprovalRuntimeDataService service = new BpmApprovalRuntimeDataService(
                Mockito.mock(BpmApprovalSubjectSnapshotDao.class),
                routingFactDao,
                Mockito.mock(BpmProcessWorkingDataDao.class)
        );

        assertThat(service.routingData(51L).facts())
                .containsEntry("amount", 9000)
                .doesNotContainKey("privateMemo");
        assertThat(service.routingData(51L).version()).isEqualTo(7L);
    }

    @Test
    void prepareForStartShouldFreezeSubjectRoutingAndWorkingDataReferences() {
        Fixture fixture = fixture();

        ApprovalRuntimeBinding binding = fixture.service.prepareForStart(101L, graphVersion(22L));

        assertThat(binding).satisfies(value -> {
            assertThat(value.approvalSubjectSnapshotId()).isEqualTo(101L);
            assertThat(value.routingFactSnapshotId()).isEqualTo(201L);
            assertThat(value.processWorkingDataId()).isEqualTo(301L);
            assertThat(value.title()).isEqualTo("设备采购申请");
            assertThat(value.businessKey()).isEqualTo("REQ-2026-0001");
            assertThat(value.workingDataJson()).contains("approvedAmount");
        });
    }

    @Test
    void prepareForStartShouldRejectSubjectFromDifferentPublishedContract() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service.prepareForStart(101L, graphVersion(99L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("业务契约版本不一致");
    }

    @Test
    void routingFactViewShouldReadOnlyFrozenAllowedFacts() {
        Fixture fixture = fixture();

        RoutingFactView view = fixture.service.routingFactView(201L);

        assertThat(view.allowedEmployeeFactKeys()).containsExactly("financeApprover");
        assertThat(view.employeeFacts()).containsEntry("financeApprover", 50L);
        assertThat(view.employeeFacts()).doesNotContainKey("hiddenEmployee");
        assertThat(view.routeFactVersion()).isEqualTo("1");
    }

    private Fixture fixture() {
        BpmApprovalSubjectSnapshotDao subjectDao = Mockito.mock(BpmApprovalSubjectSnapshotDao.class);
        BpmRoutingFactSnapshotDao routingDao = Mockito.mock(BpmRoutingFactSnapshotDao.class);
        BpmProcessWorkingDataDao workingDao = Mockito.mock(BpmProcessWorkingDataDao.class);
        BpmApprovalSubjectSnapshotEntity subject = new BpmApprovalSubjectSnapshotEntity();
        subject.setApprovalSubjectSnapshotId(101L);
        subject.setBusinessContractVersionId(22L);
        subject.setSourceSystem("HUNYUAN");
        subject.setBusinessType("GENERIC_APPLICATION");
        subject.setBusinessKey("REQ-2026-0001");
        subject.setTitle("设备采购申请");
        subject.setSummary("申请采购研发设备");
        subject.setSnapshotState("ACTIVE");
        when(subjectDao.selectById(101L)).thenReturn(subject);

        BpmRoutingFactSnapshotEntity routing = new BpmRoutingFactSnapshotEntity();
        routing.setRoutingFactSnapshotId(201L);
        routing.setApprovalSubjectSnapshotId(101L);
        routing.setBusinessContractVersionId(22L);
        routing.setRoutingFactVersion(1L);
        routing.setAllowedFactKeysJson("[\"financeApprover\"]");
        routing.setFactsJson("{\"financeApprover\":50,\"hiddenEmployee\":99}");
        when(routingDao.selectLatestBySubject(101L)).thenReturn(routing);
        when(routingDao.selectById(201L)).thenReturn(routing);

        BpmProcessWorkingDataEntity working = new BpmProcessWorkingDataEntity();
        working.setProcessWorkingDataId(301L);
        working.setApprovalSubjectSnapshotId(101L);
        working.setDataVersion(1L);
        working.setDataJson("{\"approvedAmount\":12000.50}");
        when(workingDao.selectLatestBySubject(101L)).thenReturn(working);
        return new Fixture(new BpmApprovalRuntimeDataService(subjectDao, routingDao, workingDao));
    }

    private GraphDefinitionVersionEntity graphVersion(Long contractVersionId) {
        GraphDefinitionVersionEntity version = new GraphDefinitionVersionEntity();
        version.setDependencyVersionsJson("""
                {"businessContract":{"contractVersionId":%d,"contractKey":"generic-application","contractVersion":1}}
                """.formatted(contractVersionId));
        return version;
    }

    private record Fixture(BpmApprovalRuntimeDataService service) {
    }
}
