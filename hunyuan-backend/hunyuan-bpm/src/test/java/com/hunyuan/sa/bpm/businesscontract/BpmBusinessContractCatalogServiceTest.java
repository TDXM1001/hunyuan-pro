package com.hunyuan.sa.bpm.businesscontract;

import com.hunyuan.sa.bpm.module.businesscontract.dao.BpmBusinessContractVersionDao;
import com.hunyuan.sa.bpm.module.businesscontract.domain.entity.BpmBusinessContractVersionEntity;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractCatalogVersion;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractDraftCommand;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractLifecycleCommand;
import com.hunyuan.sa.bpm.module.businesscontract.service.BpmBusinessContractCatalogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmBusinessContractCatalogServiceTest {

    @Test
    void createDraftShouldCanonicalizeAndAllocateNextImmutableVersion() {
        BpmBusinessContractVersionDao dao = Mockito.mock(BpmBusinessContractVersionDao.class);
        when(dao.selectMaxContractVersion("generic-application")).thenReturn(2);
        BpmBusinessContractCatalogService service = new BpmBusinessContractCatalogService(dao);

        BusinessContractCatalogVersion result = service.createDraft(new BusinessContractDraftCommand(
                "generic-application", 1, validContractJson(), 99L
        ));

        ArgumentCaptor<BpmBusinessContractVersionEntity> inserted =
                ArgumentCaptor.forClass(BpmBusinessContractVersionEntity.class);
        verify(dao).insert(inserted.capture());
        assertThat(inserted.getValue()).satisfies(entity -> {
            assertThat(entity.getContractVersion()).isEqualTo(3);
            assertThat(entity.getLifecycleState()).isEqualTo("DRAFT");
            assertThat(entity.getSchemaVersion()).isEqualTo(1);
            assertThat(entity.getContractDigest()).hasSize(64);
            assertThat(entity.getCatalogRevision()).isZero();
            assertThat(entity.getCreatedByEmployeeId()).isEqualTo(99L);
            assertThat(entity.getContractJson()).startsWith("{").contains("businessType");
        });
        assertThat(result.contractVersion()).isEqualTo(3);
        assertThat(result.lifecycleState()).isEqualTo("DRAFT");
    }

    @Test
    void activateShouldUseCatalogRevisionCasAndPreserveCanonicalPayload() {
        BpmBusinessContractVersionDao dao = Mockito.mock(BpmBusinessContractVersionDao.class);
        BpmBusinessContractVersionEntity draft = draftEntity();
        draft.setCatalogRevision(2L);
        when(dao.selectByKeyAndVersionForUpdate("generic-application", 3)).thenReturn(draft);
        when(dao.transitionState(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()
        )).thenReturn(1);
        BpmBusinessContractCatalogService service = new BpmBusinessContractCatalogService(dao);

        BusinessContractCatalogVersion result = service.activate(new BusinessContractLifecycleCommand(
                "generic-application", 3, 2L, 88L
        ));

        assertThat(result.lifecycleState()).isEqualTo("ACTIVE");
        assertThat(result.catalogRevision()).isEqualTo(3L);
        assertThat(result.contractDigest()).hasSize(64);
    }

    @Test
    void activateShouldRejectMovedCatalogRevision() {
        BpmBusinessContractVersionDao dao = Mockito.mock(BpmBusinessContractVersionDao.class);
        BpmBusinessContractVersionEntity draft = draftEntity();
        draft.setCatalogRevision(3L);
        when(dao.selectByKeyAndVersionForUpdate("generic-application", 3)).thenReturn(draft);
        BpmBusinessContractCatalogService service = new BpmBusinessContractCatalogService(dao);

        assertThatThrownBy(() -> service.activate(new BusinessContractLifecycleCommand(
                "generic-application", 3, 2L, 88L
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("版本已变更");
    }

    @Test
    void validateShouldRejectEditableFieldOutsideWorkingDataSchema() {
        BpmBusinessContractCatalogService service = new BpmBusinessContractCatalogService(
                Mockito.mock(BpmBusinessContractVersionDao.class)
        );
        String invalid = validContractJson().replace(
                "[\"approvedAmount\",\"approvalNote\"]",
                "[\"approvedAmount\",\"businessAmount\"]"
        );

        assertThatThrownBy(() -> service.validate(1, invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("businessAmount");
    }

    private BpmBusinessContractVersionEntity draftEntity() {
        BpmBusinessContractVersionEntity entity = new BpmBusinessContractVersionEntity();
        entity.setBusinessContractVersionId(22L);
        entity.setContractKey("generic-application");
        entity.setContractVersion(3);
        entity.setLifecycleState("DRAFT");
        entity.setSchemaVersion(1);
        entity.setContractJson(validContractJson());
        entity.setCreatedByEmployeeId(99L);
        return entity;
    }

    private String validContractJson() {
        return """
                {
                  "sourceSystem":"HUNYUAN",
                  "businessType":"GENERIC_APPLICATION",
                  "businessKeyRule":{"pattern":"REQ-[0-9]{4}-[0-9]{4}"},
                  "fieldSchema":[
                    {"key":"amount","type":"DECIMAL","required":true,"sensitivity":"INTERNAL"},
                    {"key":"applicantNote","type":"STRING","required":false,"sensitivity":"INTERNAL"}
                  ],
                  "routingFacts":[
                    {"key":"financeApprover","type":"EMPLOYEE_ID","required":true,"sensitivity":"INTERNAL","candidateUsable":true}
                  ],
                  "workingDataSchema":[
                    {"key":"approvedAmount","type":"DECIMAL","required":true,"sensitivity":"INTERNAL"},
                    {"key":"approvalNote","type":"STRING","required":false,"sensitivity":"INTERNAL"}
                  ],
                  "attachmentRules":{"maxCount":5},
                  "detailLayout":{"sections":["fields","lineItems","attachments"]},
                  "changePolicy":{"mode":"FIELD_CONTROLLED","editableFields":["approvedAmount","approvalNote"]}
                }
                """;
    }
}
