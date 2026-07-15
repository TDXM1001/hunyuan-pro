package com.hunyuan.sa.bpm.model;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.engine.graph.GraphEdge;
import com.hunyuan.sa.bpm.engine.graph.GraphNode;
import com.hunyuan.sa.bpm.engine.graph.GraphNodeType;
import com.hunyuan.sa.bpm.engine.graph.GraphScope;
import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;
import com.hunyuan.sa.bpm.module.category.dao.BpmCategoryDao;
import com.hunyuan.sa.bpm.module.category.domain.entity.BpmCategoryEntity;
import com.hunyuan.sa.bpm.module.model.dao.BpmProcessDraftDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmProcessDraftEntity;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftCreateCommand;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftImportCommand;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftSaveCommand;
import com.hunyuan.sa.bpm.module.model.domain.vo.BpmGraphDraftVO;
import com.hunyuan.sa.bpm.module.model.service.BpmGraphDraftService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class BpmGraphDraftServiceTest {

    private BpmGraphDraftService service;

    private BpmProcessDraftDao draftDao;

    private BpmCategoryDao categoryDao;

    @BeforeEach
    void setUp() {
        service = new BpmGraphDraftService();
        draftDao = Mockito.mock(BpmProcessDraftDao.class);
        categoryDao = Mockito.mock(BpmCategoryDao.class);
        setField(service, "bpmProcessDraftDao", draftDao);
        setField(service, "bpmCategoryDao", categoryDao);
        BpmCategoryEntity category = new BpmCategoryEntity();
        category.setCategoryId(9L);
        category.setDisabledFlag(false);
        category.setDeletedFlag(false);
        when(categoryDao.selectById(9L)).thenReturn(category);
    }

    @Test
    void createShouldPersistCanonicalGraphAndIndependentLayout() {
        when(draftDao.insert(any(BpmProcessDraftEntity.class))).thenAnswer(invocation -> {
            BpmProcessDraftEntity entity = invocation.getArgument(0);
            entity.setDraftId(100L);
            return 1;
        });

        ResponseDTO<BpmGraphDraftVO> response = service.createDraft(new BpmGraphDraftCreateCommand(
                "expense_apply",
                "报销申请",
                9L,
                graph(120),
                7L
        ));

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmProcessDraftEntity> captor = ArgumentCaptor.forClass(BpmProcessDraftEntity.class);
        Mockito.verify(draftDao).insert(captor.capture());
        BpmProcessDraftEntity saved = captor.getValue();
        assertThat(saved.getRevision()).isEqualTo(1);
        assertThat(saved.getGraphJson()).contains("node_review").doesNotContain("\"layout\"");
        assertThat(saved.getLayoutJson()).contains("node_review", "120");
        assertThat(saved.getSemanticHash()).hasSize(64);
    }

    @Test
    void createShouldRejectDuplicateProcessKeyBeforeInsert() {
        BpmProcessDraftEntity existing = new BpmProcessDraftEntity();
        existing.setDraftId(99L);
        when(draftDao.selectByProcessKey("expense_apply")).thenReturn(existing);

        ResponseDTO<BpmGraphDraftVO> response = service.createDraft(new BpmGraphDraftCreateCommand(
                "expense_apply",
                "报销申请",
                9L,
                graph(120),
                7L
        ));

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("编码已存在");
        Mockito.verify(draftDao, Mockito.never()).insert(any(BpmProcessDraftEntity.class));
    }

    @Test
    void createShouldRejectMissingCategoryBeforeInsert() {
        ResponseDTO<BpmGraphDraftVO> response = service.createDraft(new BpmGraphDraftCreateCommand(
                "expense_apply",
                "报销申请",
                null,
                graph(120),
                7L
        ));

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("流程分类");
        Mockito.verify(draftDao, Mockito.never()).insert(any(BpmProcessDraftEntity.class));
    }

    @Test
    void createShouldRejectUnavailableCategoryBeforeInsert() {
        BpmCategoryEntity disabled = new BpmCategoryEntity();
        disabled.setCategoryId(10L);
        disabled.setDisabledFlag(true);
        disabled.setDeletedFlag(false);
        when(categoryDao.selectById(10L)).thenReturn(disabled);

        ResponseDTO<BpmGraphDraftVO> response = service.createDraft(new BpmGraphDraftCreateCommand(
                "expense_apply",
                "报销申请",
                10L,
                graph(120),
                7L
        ));

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("不可用");
        Mockito.verify(draftDao, Mockito.never()).insert(any(BpmProcessDraftEntity.class));
    }

    @Test
    void saveShouldRejectStaleRevisionWithoutOverwritingDraft() {
        when(draftDao.updateIfRevision(anyLong(), anyInt(), any(), any(), any(), anyLong())).thenReturn(0);

        ResponseDTO<BpmGraphDraftVO> response = service.saveDraft(new BpmGraphDraftSaveCommand(
                100L,
                3,
                graph(300),
                8L
        ));

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("版本已变更");
    }

    @Test
    void layoutOnlySaveShouldKeepSemanticHashAndAdvanceRevision() {
        when(draftDao.updateIfRevision(anyLong(), anyInt(), any(), any(), any(), anyLong())).thenReturn(1);

        ResponseDTO<BpmGraphDraftVO> response = service.saveDraft(new BpmGraphDraftSaveCommand(
                100L,
                3,
                graph(600),
                8L
        ));

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getRevision()).isEqualTo(4);
        assertThat(response.getData().getLayoutJson()).contains("600");
        assertThat(response.getData().getSemanticHash())
                .isEqualTo(service.semanticHash(graph(120)));
    }

    @Test
    void detailShouldReturnPersistedRevisionFact() {
        BpmProcessDraftEntity entity = new BpmProcessDraftEntity();
        entity.setDraftId(100L);
        entity.setRevision(4);
        entity.setGraphJson("{\"nodes\":[]}");
        entity.setLayoutJson("{\"node_review\":{\"x\":600}}");
        entity.setSemanticHash("a".repeat(64));
        when(draftDao.selectById(100L)).thenReturn(entity);

        ResponseDTO<BpmGraphDraftVO> response = service.getDraft(100L);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getRevision()).isEqualTo(4);
        assertThat(response.getData().getSemanticHash()).isEqualTo("a".repeat(64));
    }

    @Test
    void exportThenImportShouldCreateDraftWithFreshGraphIds() {
        HunyuanProcessDefinitionGraph source = graph(120);
        BpmProcessDraftEntity sourceEntity = new BpmProcessDraftEntity();
        sourceEntity.setDraftId(100L);
        sourceEntity.setGraphJson(service.canonicalGraphJson(source));
        sourceEntity.setLayoutJson(JSON.toJSONString(Map.of(
                "node_start", Map.of("x", 0, "y", 0),
                "node_review", Map.of("x", 120, "y", 0),
                "node_end", Map.of("x", 800, "y", 0)
        )));
        when(draftDao.selectById(100L)).thenReturn(sourceEntity);
        when(draftDao.insert(any(BpmProcessDraftEntity.class))).thenAnswer(invocation -> {
            BpmProcessDraftEntity entity = invocation.getArgument(0);
            entity.setDraftId(101L);
            return 1;
        });

        ResponseDTO<String> exported = service.exportDraft(100L);
        ResponseDTO<BpmGraphDraftVO> imported = service.importDraft(new BpmGraphDraftImportCommand(
                "expense_apply_copy", "报销申请副本", 9L, exported.getData(), 8L
        ));

        assertThat(exported.getOk()).isTrue();
        assertThat(imported.getOk()).isTrue();
        ArgumentCaptor<BpmProcessDraftEntity> captor = ArgumentCaptor.forClass(BpmProcessDraftEntity.class);
        Mockito.verify(draftDao).insert(captor.capture());
        assertThat(captor.getValue().getGraphJson()).doesNotContain("node_review");
        assertThat(captor.getValue().getGraphJson()).contains("node_");
    }

    private HunyuanProcessDefinitionGraph graph(int reviewX) {
        return new HunyuanProcessDefinitionGraph(
                1,
                "scope_root",
                List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(
                        new GraphNode("node_start", "scope_root", GraphNodeType.START, "开始", Map.of(), Map.of("x", 0, "y", 0)),
                        new GraphNode("node_review", "scope_root", GraphNodeType.APPROVAL, "主管审批", Map.of("strategyRef", "manager"), Map.of("x", reviewX, "y", 0)),
                        new GraphNode("node_end", "scope_root", GraphNodeType.END, "结束", Map.of(), Map.of("x", 800, "y", 0))
                ),
                List.of(
                        new GraphEdge("edge_start_review", "scope_root", "node_start", "node_review", "default", Map.of()),
                        new GraphEdge("edge_review_end", "scope_root", "node_review", "node_end", "default", Map.of())
                ),
                Map.of()
        );
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
