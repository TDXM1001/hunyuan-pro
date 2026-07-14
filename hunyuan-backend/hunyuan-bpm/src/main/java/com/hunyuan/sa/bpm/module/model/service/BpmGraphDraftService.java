package com.hunyuan.sa.bpm.module.model.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartPageUtil;
import com.hunyuan.sa.bpm.engine.graph.GraphCanonicalizer;
import com.hunyuan.sa.bpm.engine.graph.GraphDocumentCodec;
import com.hunyuan.sa.bpm.engine.graph.GraphNode;
import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;
import com.hunyuan.sa.bpm.module.model.dao.BpmProcessDraftDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmProcessDraftEntity;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftCreateCommand;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftImportCommand;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftQueryForm;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftSaveCommand;
import com.hunyuan.sa.bpm.module.model.domain.vo.BpmGraphDraftVO;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;
import java.util.List;

/**
 * Graph 草稿的创建与 revision 条件保存，不读取或写入旧作者模型。
 */
@Service
public class BpmGraphDraftService {

    private static final String DRAFT_STATUS = "DRAFT";

    private final GraphCanonicalizer graphCanonicalizer = new GraphCanonicalizer();

    private final GraphDocumentCodec graphDocumentCodec = new GraphDocumentCodec();

    @Resource
    private BpmProcessDraftDao bpmProcessDraftDao;

    public ResponseDTO<PageResult<BpmGraphDraftVO>> queryDrafts(BpmGraphDraftQueryForm form) {
        Page<BpmProcessDraftEntity> page = new Page<>(form.getPageNum(), form.getPageSize());
        page.setSearchCount(!Boolean.FALSE.equals(form.getSearchCount()));
        Page<BpmProcessDraftEntity> result = bpmProcessDraftDao.selectPage(
                page,
                Wrappers.<BpmProcessDraftEntity>lambdaQuery()
                        .like(org.springframework.util.StringUtils.hasText(form.getProcessKey()),
                                BpmProcessDraftEntity::getProcessKey, form.getProcessKey())
                        .like(org.springframework.util.StringUtils.hasText(form.getProcessName()),
                                BpmProcessDraftEntity::getProcessName, form.getProcessName())
                        .eq(form.getCategoryId() != null,
                                BpmProcessDraftEntity::getCategoryId, form.getCategoryId())
                        .orderByDesc(BpmProcessDraftEntity::getUpdateTime, BpmProcessDraftEntity::getDraftId)
        );
        List<BpmGraphDraftVO> records = result.getRecords().stream().map(this::toSummaryVO).toList();
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(result, records));
    }

    public ResponseDTO<BpmGraphDraftVO> createDraft(BpmGraphDraftCreateCommand command) {
        if (bpmProcessDraftDao.selectByProcessKey(command.processKey()) != null) {
            return ResponseDTO.userErrorParam("流程资产编码已存在");
        }
        String graphJson = graphCanonicalizer.canonicalize(command.graph());
        String layoutJson = layoutJson(command.graph());
        String semanticHash = graphCanonicalizer.semanticHash(command.graph());

        BpmProcessDraftEntity entity = new BpmProcessDraftEntity();
        entity.setProcessKey(command.processKey());
        entity.setProcessName(command.processName());
        entity.setCategoryId(command.categoryId());
        entity.setRevision(1);
        entity.setGraphJson(graphJson);
        entity.setLayoutJson(layoutJson);
        entity.setSemanticHash(semanticHash);
        entity.setDraftStatus(DRAFT_STATUS);
        entity.setCreatedByEmployeeId(command.operatorEmployeeId());
        entity.setUpdatedByEmployeeId(command.operatorEmployeeId());
        bpmProcessDraftDao.insert(entity);
        return ResponseDTO.ok(toVO(entity.getDraftId(), 1, graphJson, layoutJson, semanticHash));
    }

    public ResponseDTO<BpmGraphDraftVO> saveDraft(BpmGraphDraftSaveCommand command) {
        String graphJson = graphCanonicalizer.canonicalize(command.graph());
        String layoutJson = layoutJson(command.graph());
        String semanticHash = graphCanonicalizer.semanticHash(command.graph());
        int affected = bpmProcessDraftDao.updateIfRevision(
                command.draftId(),
                command.expectedRevision(),
                graphJson,
                layoutJson,
                semanticHash,
                command.operatorEmployeeId()
        );
        if (affected != 1) {
            return ResponseDTO.userErrorParam("流程草稿版本已变更，请刷新后重试");
        }
        return ResponseDTO.ok(toVO(
                command.draftId(),
                command.expectedRevision() + 1,
                graphJson,
                layoutJson,
                semanticHash
        ));
    }

    public ResponseDTO<BpmGraphDraftVO> getDraft(Long draftId) {
        BpmProcessDraftEntity entity = bpmProcessDraftDao.selectById(draftId);
        if (entity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return ResponseDTO.ok(toVO(
                entity.getDraftId(),
                entity.getRevision(),
                entity.getGraphJson(),
                entity.getLayoutJson(),
                entity.getSemanticHash()
        ));
    }

    public ResponseDTO<String> exportDraft(Long draftId) {
        BpmProcessDraftEntity entity = bpmProcessDraftDao.selectById(draftId);
        if (entity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return ResponseDTO.ok(graphDocumentCodec.export(
                graphDocumentCodec.restoreStored(entity.getGraphJson(), entity.getLayoutJson())
        ));
    }

    public ResponseDTO<BpmGraphDraftVO> importDraft(BpmGraphDraftImportCommand command) {
        return createDraft(new BpmGraphDraftCreateCommand(
                command.processKey(),
                command.processName(),
                command.categoryId(),
                graphDocumentCodec.importAsNewGraph(command.graphDocumentJson()),
                command.operatorEmployeeId()
        ));
    }

    public String semanticHash(HunyuanProcessDefinitionGraph graph) {
        return graphCanonicalizer.semanticHash(graph);
    }

    public String canonicalGraphJson(HunyuanProcessDefinitionGraph graph) {
        return graphCanonicalizer.canonicalize(graph);
    }

    private String layoutJson(HunyuanProcessDefinitionGraph graph) {
        Map<String, Object> layouts = new TreeMap<>();
        for (GraphNode node : graph.nodes()) {
            layouts.put(node.nodeId(), node.layout());
        }
        return JSON.toJSONString(layouts);
    }

    private BpmGraphDraftVO toVO(
            Long draftId,
            int revision,
            String graphJson,
            String layoutJson,
            String semanticHash
    ) {
        BpmGraphDraftVO result = new BpmGraphDraftVO();
        result.setDraftId(draftId);
        result.setRevision(revision);
        result.setGraphJson(graphJson);
        result.setLayoutJson(layoutJson);
        result.setSemanticHash(semanticHash);
        return result;
    }

    private BpmGraphDraftVO toSummaryVO(BpmProcessDraftEntity entity) {
        BpmGraphDraftVO result = toVO(
                entity.getDraftId(), entity.getRevision(), entity.getGraphJson(),
                entity.getLayoutJson(), entity.getSemanticHash()
        );
        result.setProcessKey(entity.getProcessKey());
        result.setProcessName(entity.getProcessName());
        result.setCategoryId(entity.getCategoryId());
        result.setDraftStatus(entity.getDraftStatus());
        result.setUpdateTime(entity.getUpdateTime());
        return result;
    }
}
