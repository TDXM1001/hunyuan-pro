package com.hunyuan.sa.bpm.businesscontract;

import com.hunyuan.sa.bpm.module.businesscontract.dao.BpmBusinessContractVersionDao;
import com.hunyuan.sa.bpm.module.businesscontract.domain.entity.BpmBusinessContractVersionEntity;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractCatalogVersion;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractDraftCommand;
import com.hunyuan.sa.bpm.module.businesscontract.domain.model.BusinessContractLifecycleCommand;
import com.hunyuan.sa.bpm.module.businesscontract.domain.vo.BpmBusinessObjectTechnicalDiffVO;
import com.hunyuan.sa.bpm.module.businesscontract.service.BpmBusinessContractCatalogService;
import com.hunyuan.sa.bpm.module.definition.domain.vo.BpmDefinitionReferenceVO;
import com.hunyuan.sa.bpm.module.definition.service.BpmDefinitionReferenceQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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

    @Test
    void staleVisualDraftSaveShouldFailCatalogRevisionCas() {
        BpmBusinessContractVersionDao dao = Mockito.mock(BpmBusinessContractVersionDao.class);
        when(dao.saveVisualDraft(
                ArgumentMatchers.eq("expense"), ArgumentMatchers.eq(2), ArgumentMatchers.eq(0L),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString()
        )).thenReturn(0);
        BpmBusinessContractCatalogService service = new BpmBusinessContractCatalogService(dao);

        assertThatThrownBy(() -> service.saveVisualDraft(2, BusinessObjectFixtures.expense(), 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CATALOG_REVISION_CONFLICT");
    }

    @Test
    void upgradeMustCreateNewV2DraftWithoutChangingV1() {
        BpmBusinessContractVersionDao dao = Mockito.mock(BpmBusinessContractVersionDao.class);
        BpmBusinessContractVersionEntity source = draftEntity();
        String sourceJson = source.getContractJson();
        source.setContractVersion(1);
        source.setContractDigest("source-digest");
        when(dao.selectOne(ArgumentMatchers.any())).thenReturn(source);
        when(dao.selectMaxContractVersion("generic-application")).thenReturn(1);
        when(dao.saveVisualDraft(
                ArgumentMatchers.eq("generic-application"), ArgumentMatchers.eq(2), ArgumentMatchers.eq(0L),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString()
        )).thenReturn(1);
        BpmBusinessContractCatalogService service = new BpmBusinessContractCatalogService(dao);

        var upgraded = service.upgradeV1AsV2Draft("generic-application", 1, 1L);

        assertThat(upgraded.schemaVersion()).isEqualTo(2);
        assertThat(upgraded.contractVersion()).isEqualTo(2);
        assertThat(source.getContractJson()).isEqualTo(sourceJson);
        assertThat(source.getContractDigest()).isEqualTo("source-digest");
        verify(dao).insert(ArgumentMatchers.<BpmBusinessContractVersionEntity>argThat(
                entity -> entity.getSchemaVersion() == 2));
        verify(dao, never()).updateById(source);
    }

    @Test
    void referencedDraftCannotBeDeletedAndTechnicalDiffIsReadOnly() {
        BpmBusinessContractVersionDao dao = Mockito.mock(BpmBusinessContractVersionDao.class);
        BpmDefinitionReferenceQueryService references = Mockito.mock(BpmDefinitionReferenceQueryService.class);
        when(references.findBusinessContractReferences("expense", 2)).thenReturn(java.util.List.of(
                new BpmDefinitionReferenceVO(9L, 7L, "DRAFT", "expense-flow", "费用审批", 1, "DRAFT")
        ));
        BpmBusinessContractCatalogService service = new BpmBusinessContractCatalogService(dao, references);

        assertThatThrownBy(() -> service.deleteDraft("expense", 2, 0L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("草稿仍被 Graph 引用");

        BpmBusinessContractVersionEntity left = v2Entity(1, "amount");
        BpmBusinessContractVersionEntity right = v2Entity(2, "approvalNote");
        when(dao.selectOne(ArgumentMatchers.any())).thenReturn(left, right);
        BpmBusinessObjectTechnicalDiffVO diff = service.technicalDiff("expense", 1, 2);
        assertThat(diff.changedFieldKeys()).containsExactly("amount", "approvalNote");
        verify(dao, never()).updateById(ArgumentMatchers.<BpmBusinessContractVersionEntity>any());
    }

    private BpmBusinessContractVersionEntity v2Entity(int version, String fieldKey) {
        BpmBusinessContractVersionEntity entity = new BpmBusinessContractVersionEntity();
        entity.setContractKey("expense");
        entity.setContractVersion(version);
        entity.setSchemaVersion(2);
        entity.setContractJson("{\"schemaVersion\":2,\"fieldSchema\":[{\"key\":\"" + fieldKey
                + "\"}],\"routingFacts\":[],\"workingDataSchema\":[]}");
        return entity;
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
