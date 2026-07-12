package com.hunyuan.sa.bpm.model;

import com.alibaba.fastjson.JSON;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.engine.graph.GraphCanonicalizer;
import com.hunyuan.sa.bpm.engine.graph.GraphEdge;
import com.hunyuan.sa.bpm.engine.graph.GraphNode;
import com.hunyuan.sa.bpm.engine.graph.GraphNodeType;
import com.hunyuan.sa.bpm.engine.graph.GraphScope;
import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;
import com.hunyuan.sa.bpm.module.model.dao.BpmProcessDraftDao;
import com.hunyuan.sa.bpm.module.model.dao.BpmProcessTemplateDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmProcessDraftEntity;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmProcessTemplateEntity;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphTemplateCopyCommand;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphTemplateCreateCommand;
import com.hunyuan.sa.bpm.module.model.domain.vo.BpmGraphDraftVO;
import com.hunyuan.sa.bpm.module.model.domain.vo.BpmGraphTemplateVO;
import com.hunyuan.sa.bpm.module.model.service.BpmGraphDraftService;
import com.hunyuan.sa.bpm.module.model.service.BpmGraphTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class BpmGraphTemplateServiceTest {

    private BpmGraphTemplateService service;

    private BpmProcessDraftDao draftDao;

    private BpmProcessTemplateDao templateDao;

    private BpmGraphDraftService graphDraftService;

    @BeforeEach
    void setUp() {
        service = new BpmGraphTemplateService();
        draftDao = Mockito.mock(BpmProcessDraftDao.class);
        templateDao = Mockito.mock(BpmProcessTemplateDao.class);
        graphDraftService = Mockito.mock(BpmGraphDraftService.class);
        setField(service, "bpmProcessDraftDao", draftDao);
        setField(service, "bpmProcessTemplateDao", templateDao);
        setField(service, "bpmGraphDraftService", graphDraftService);
    }

    @Test
    void createTemplateShouldFreezeSourceDraftSnapshot() {
        BpmProcessDraftEntity source = sourceDraft();
        when(draftDao.selectById(100L)).thenReturn(source);
        when(templateDao.insert(any(BpmProcessTemplateEntity.class))).thenAnswer(invocation -> {
            invocation.<BpmProcessTemplateEntity>getArgument(0).setTemplateId(200L);
            return 1;
        });

        ResponseDTO<BpmGraphTemplateVO> response = service.createTemplate(new BpmGraphTemplateCreateCommand(
                "expense_template", "报销模板", 100L, 9L
        ));

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<BpmProcessTemplateEntity> captor = ArgumentCaptor.forClass(BpmProcessTemplateEntity.class);
        Mockito.verify(templateDao).insert(captor.capture());
        assertThat(captor.getValue().getGraphJson()).isEqualTo(source.getGraphJson());
        assertThat(captor.getValue().getLayoutJson()).isEqualTo(source.getLayoutJson());
    }

    @Test
    void copyTemplateShouldPassFreshIdsToNewDraft() {
        BpmProcessTemplateEntity template = new BpmProcessTemplateEntity();
        template.setTemplateId(200L);
        template.setGraphJson(sourceDraft().getGraphJson());
        template.setLayoutJson(sourceDraft().getLayoutJson());
        template.setEnabledFlag(Boolean.TRUE);
        when(templateDao.selectById(200L)).thenReturn(template);
        when(graphDraftService.createDraft(any())).thenReturn(ResponseDTO.ok(new BpmGraphDraftVO()));

        ResponseDTO<BpmGraphDraftVO> response = service.copyTemplate(new BpmGraphTemplateCopyCommand(
                200L, "expense_copy", "报销副本", 9L, 9L
        ));

        assertThat(response.getOk()).isTrue();
        ArgumentCaptor<com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftCreateCommand> captor =
                ArgumentCaptor.forClass(com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftCreateCommand.class);
        Mockito.verify(graphDraftService).createDraft(captor.capture());
        assertThat(captor.getValue().graph().nodes()).extracting(GraphNode::nodeId)
                .doesNotContain("node_start", "node_review", "node_end");
    }

    private BpmProcessDraftEntity sourceDraft() {
        HunyuanProcessDefinitionGraph graph = new HunyuanProcessDefinitionGraph(
                1, "scope_root", List.of(new GraphScope("scope_root", null, "主流程")),
                List.of(
                        new GraphNode("node_start", "scope_root", GraphNodeType.START, "开始", Map.of(), Map.of("x", 0, "y", 0)),
                        new GraphNode("node_review", "scope_root", GraphNodeType.APPROVAL, "主管审批", Map.of(), Map.of("x", 200, "y", 0)),
                        new GraphNode("node_end", "scope_root", GraphNodeType.END, "结束", Map.of(), Map.of("x", 400, "y", 0))
                ),
                List.of(
                        new GraphEdge("edge_start_review", "scope_root", "node_start", "node_review", "default", Map.of()),
                        new GraphEdge("edge_review_end", "scope_root", "node_review", "node_end", "default", Map.of())
                ), Map.of()
        );
        BpmProcessDraftEntity result = new BpmProcessDraftEntity();
        result.setDraftId(100L);
        result.setGraphJson(new GraphCanonicalizer().canonicalize(graph));
        result.setLayoutJson(JSON.toJSONString(Map.of(
                "node_start", Map.of("x", 0, "y", 0),
                "node_review", Map.of("x", 200, "y", 0),
                "node_end", Map.of("x", 400, "y", 0)
        )));
        result.setSemanticHash(new GraphCanonicalizer().semanticHash(graph));
        return result;
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
