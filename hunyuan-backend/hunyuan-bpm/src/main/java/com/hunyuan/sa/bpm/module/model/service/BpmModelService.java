package com.hunyuan.sa.bpm.module.model.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.common.util.SmartPageUtil;
import com.hunyuan.sa.bpm.module.category.dao.BpmCategoryDao;
import com.hunyuan.sa.bpm.module.category.domain.entity.BpmCategoryEntity;
import com.hunyuan.sa.bpm.module.form.dao.BpmFormDao;
import com.hunyuan.sa.bpm.module.form.domain.entity.BpmFormEntity;
import com.hunyuan.sa.bpm.module.model.dao.BpmModelDao;
import com.hunyuan.sa.bpm.module.model.domain.entity.BpmModelEntity;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmModelAddForm;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmModelQueryForm;
import com.hunyuan.sa.bpm.module.model.domain.form.BpmModelUpdateForm;
import com.hunyuan.sa.bpm.module.model.domain.vo.BpmModelVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程模型服务。
 */
@Service
public class BpmModelService {

    private static final String DEFAULT_SIMPLE_MODEL_JSON = "{\"nodes\":[]}";

    private static final String DEFAULT_START_RULE_JSON = "{\"allowAll\":true}";

    @Resource
    private BpmModelDao bpmModelDao;

    @Resource
    private BpmCategoryDao bpmCategoryDao;

    @Resource
    private BpmFormDao bpmFormDao;

    public ResponseDTO<PageResult<BpmModelVO>> queryModel(BpmModelQueryForm queryForm) {
        queryForm.setDeletedFlag(Boolean.FALSE);
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<BpmModelVO> list = bpmModelDao.queryPage(page, queryForm);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, list));
    }

    public ResponseDTO<String> addModel(BpmModelAddForm addForm) {
        BpmModelEntity existEntity = new BpmModelEntity();
        existEntity.setModelKey(addForm.getModelKey());
        existEntity.setDeletedFlag(Boolean.FALSE);
        if (bpmModelDao.selectOne(existEntity) != null) {
            return ResponseDTO.userErrorParam("流程模型编码已存在");
        }

        ResponseDTO<String> relationResponse = checkRelation(addForm.getCategoryId(), addForm.getFormId());
        if (!Boolean.TRUE.equals(relationResponse.getOk())) {
            return relationResponse;
        }

        BpmModelEntity entity = SmartBeanUtil.copy(addForm, BpmModelEntity.class);
        entity.setVisibleFlag(!Boolean.FALSE.equals(addForm.getVisibleFlag()));
        entity.setSort(addForm.getSort() == null ? 0 : addForm.getSort());
        entity.setSimpleModelJson(DEFAULT_SIMPLE_MODEL_JSON);
        entity.setStartRuleJson(DEFAULT_START_RULE_JSON);
        entity.setHasUnpublishedChanges(Boolean.TRUE);
        entity.setDeletedFlag(Boolean.FALSE);
        bpmModelDao.insert(entity);
        return ResponseDTO.ok();
    }

    public ResponseDTO<String> updateModel(BpmModelUpdateForm updateForm) {
        BpmModelEntity dbEntity = bpmModelDao.selectById(updateForm.getModelId());
        if (dbEntity == null || Boolean.TRUE.equals(dbEntity.getDeletedFlag())) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        BpmModelEntity existEntity = new BpmModelEntity();
        existEntity.setModelKey(updateForm.getModelKey());
        existEntity.setDeletedFlag(Boolean.FALSE);
        BpmModelEntity duplicatedEntity = bpmModelDao.selectOne(existEntity);
        if (duplicatedEntity != null && !duplicatedEntity.getModelId().equals(updateForm.getModelId())) {
            return ResponseDTO.userErrorParam("流程模型编码已存在");
        }

        ResponseDTO<String> relationResponse = checkRelation(updateForm.getCategoryId(), updateForm.getFormId());
        if (!Boolean.TRUE.equals(relationResponse.getOk())) {
            return relationResponse;
        }

        BpmModelEntity entity = SmartBeanUtil.copy(updateForm, BpmModelEntity.class);
        entity.setVisibleFlag(!Boolean.FALSE.equals(updateForm.getVisibleFlag()));
        entity.setSort(updateForm.getSort() == null ? 0 : updateForm.getSort());
        entity.setSimpleModelJson(dbEntity.getSimpleModelJson());
        entity.setStartRuleJson(dbEntity.getStartRuleJson());
        entity.setManagerScopeJson(dbEntity.getManagerScopeJson());
        entity.setTitleRuleJson(dbEntity.getTitleRuleJson());
        entity.setSummaryRuleJson(dbEntity.getSummaryRuleJson());
        entity.setVariableMappingJson(dbEntity.getVariableMappingJson());
        entity.setPublishedDefinitionId(dbEntity.getPublishedDefinitionId());
        entity.setHasUnpublishedChanges(dbEntity.getHasUnpublishedChanges());
        entity.setDeletedFlag(dbEntity.getDeletedFlag());
        bpmModelDao.updateById(entity);
        return ResponseDTO.ok();
    }

    public ResponseDTO<BpmModelVO> getModelDetail(Long modelId) {
        BpmModelVO detail = bpmModelDao.queryModelDetail(modelId);
        if (detail == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return ResponseDTO.ok(detail);
    }

    private ResponseDTO<String> checkRelation(Long categoryId, Long formId) {
        BpmCategoryEntity categoryEntity = bpmCategoryDao.selectById(categoryId);
        if (categoryEntity == null || Boolean.TRUE.equals(categoryEntity.getDeletedFlag())) {
            return ResponseDTO.userErrorParam("流程分类不存在");
        }
        BpmFormEntity formEntity = bpmFormDao.selectById(formId);
        if (formEntity == null || Boolean.TRUE.equals(formEntity.getDeletedFlag())) {
            return ResponseDTO.userErrorParam("流程表单不存在");
        }
        return ResponseDTO.ok();
    }
}
