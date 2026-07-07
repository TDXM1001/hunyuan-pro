package com.hunyuan.sa.bpm.module.model.service;

import jakarta.annotation.Resource;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.engine.compiler.SimpleModelValidator;
import com.hunyuan.sa.bpm.module.form.dao.BpmFormDao;
import com.hunyuan.sa.bpm.module.model.dao.BpmModelDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmModelEntity;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmDesignerSaveForm;
import com.hunyuan.sa.bpm.module.model.domain.vo.BpmDesignerDetailVO;
import org.springframework.stereotype.Service;

/**
 * 流程设计器服务。
 */
@Service
public class BpmDesignerService {

    @Resource
    private BpmModelDao bpmModelDao;

    @Resource
    private BpmFormDao bpmFormDao;

    @Resource
    private SimpleModelValidator simpleModelValidator;

    public ResponseDTO<BpmDesignerDetailVO> getDesignerDetail(Long modelId) {
        BpmDesignerDetailVO detail = bpmModelDao.queryDesignerDetail(modelId);
        if (detail == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return ResponseDTO.ok(detail);
    }

    public ResponseDTO<String> saveDesignerDraft(BpmDesignerSaveForm saveForm) {
        BpmModelEntity entity = bpmModelDao.selectById(saveForm.getModelId());
        if (entity == null || Boolean.TRUE.equals(entity.getDeletedFlag())) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        entity.setSimpleModelJson(saveForm.getSimpleModelJson());
        entity.setStartRuleJson(saveForm.getStartRuleJson());
        entity.setManagerScopeJson(saveForm.getManagerScopeJson());
        entity.setTitleRuleJson(saveForm.getTitleRuleJson());
        entity.setSummaryRuleJson(saveForm.getSummaryRuleJson());
        entity.setVariableMappingJson(saveForm.getVariableMappingJson());
        entity.setHasUnpublishedChanges(Boolean.TRUE);
        bpmModelDao.updateById(entity);
        return ResponseDTO.ok();
    }

    public ResponseDTO<String> validateDesignerDraft(Long modelId) {
        BpmModelEntity entity = bpmModelDao.selectById(modelId);
        if (entity == null || Boolean.TRUE.equals(entity.getDeletedFlag())) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return simpleModelValidator.validate(entity.getSimpleModelJson(), entity.getStartRuleJson());
    }

    public ResponseDTO<String> simulateDesignerDraft(Long modelId) {
        BpmModelEntity entity = bpmModelDao.selectById(modelId);
        if (entity == null || Boolean.TRUE.equals(entity.getDeletedFlag())) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return simpleModelValidator.simulate(entity.getSimpleModelJson(), entity.getStartRuleJson());
    }
}
