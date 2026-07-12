package com.hunyuan.sa.bpm.module.model.service;

import jakarta.annotation.Resource;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.engine.graph.GraphDocumentCodec;
import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;
import com.hunyuan.sa.bpm.module.model.dao.BpmProcessDraftDao;
import com.hunyuan.sa.bpm.module.model.dao.BpmProcessTemplateDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmProcessDraftEntity;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmProcessTemplateEntity;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphDraftCreateCommand;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphTemplateCopyCommand;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmGraphTemplateCreateCommand;
import com.hunyuan.sa.bpm.module.model.domain.vo.BpmGraphDraftVO;
import com.hunyuan.sa.bpm.module.model.domain.vo.BpmGraphTemplateVO;
import org.springframework.stereotype.Service;

/**
 * Graph 模板的冻结和复制；模板不与后续草稿形成双向同步。
 */
@Service
public class BpmGraphTemplateService {

    private final GraphDocumentCodec graphDocumentCodec = new GraphDocumentCodec();

    @Resource
    private BpmProcessDraftDao bpmProcessDraftDao;

    @Resource
    private BpmProcessTemplateDao bpmProcessTemplateDao;

    @Resource
    private BpmGraphDraftService bpmGraphDraftService;

    public ResponseDTO<BpmGraphTemplateVO> createTemplate(BpmGraphTemplateCreateCommand command) {
        if (bpmProcessTemplateDao.selectByTemplateKey(command.templateKey()) != null) {
            return ResponseDTO.userErrorParam("流程模板编码已存在");
        }
        BpmProcessDraftEntity source = bpmProcessDraftDao.selectById(command.sourceDraftId());
        if (source == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        BpmProcessTemplateEntity template = new BpmProcessTemplateEntity();
        template.setTemplateKey(command.templateKey());
        template.setTemplateName(command.templateName());
        template.setCategoryId(source.getCategoryId());
        template.setSourceDraftId(source.getDraftId());
        template.setGraphJson(source.getGraphJson());
        template.setLayoutJson(source.getLayoutJson());
        template.setSemanticHash(source.getSemanticHash());
        template.setEnabledFlag(Boolean.TRUE);
        template.setCreatedByEmployeeId(command.operatorEmployeeId());
        template.setUpdatedByEmployeeId(command.operatorEmployeeId());
        bpmProcessTemplateDao.insert(template);
        return ResponseDTO.ok(toVO(template));
    }

    public ResponseDTO<BpmGraphDraftVO> copyTemplate(BpmGraphTemplateCopyCommand command) {
        BpmProcessTemplateEntity template = bpmProcessTemplateDao.selectById(command.templateId());
        if (template == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (!Boolean.TRUE.equals(template.getEnabledFlag())) {
            return ResponseDTO.userErrorParam("流程模板已停用");
        }
        HunyuanProcessDefinitionGraph source = graphDocumentCodec.restoreStored(
                template.getGraphJson(), template.getLayoutJson()
        );
        HunyuanProcessDefinitionGraph copied = graphDocumentCodec.copyWithFreshIds(source);
        return bpmGraphDraftService.createDraft(new BpmGraphDraftCreateCommand(
                command.processKey(),
                command.processName(),
                command.categoryId(),
                copied,
                command.operatorEmployeeId()
        ));
    }

    private BpmGraphTemplateVO toVO(BpmProcessTemplateEntity entity) {
        BpmGraphTemplateVO result = new BpmGraphTemplateVO();
        result.setTemplateId(entity.getTemplateId());
        result.setSourceDraftId(entity.getSourceDraftId());
        result.setTemplateKey(entity.getTemplateKey());
        result.setTemplateName(entity.getTemplateName());
        result.setSemanticHash(entity.getSemanticHash());
        return result;
    }
}
