package com.hunyuan.sa.bpm.module.form.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.common.util.SmartPageUtil;
import com.hunyuan.sa.bpm.module.form.dao.BpmFormDao;
import com.hunyuan.sa.bpm.module.form.domain.entity.BpmFormEntity;
import com.hunyuan.sa.bpm.module.form.domain.form.BpmFormAddForm;
import com.hunyuan.sa.bpm.module.form.domain.form.BpmFormQueryForm;
import com.hunyuan.sa.bpm.module.form.domain.form.BpmFormUpdateForm;
import com.hunyuan.sa.bpm.module.form.domain.vo.BpmFormVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程表单服务。
 */
@Service
public class BpmFormService {

    @Resource
    private BpmFormDao bpmFormDao;

    public ResponseDTO<PageResult<BpmFormVO>> queryForm(BpmFormQueryForm queryForm) {
        queryForm.setDeletedFlag(Boolean.FALSE);
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<BpmFormVO> list = bpmFormDao.queryPage(page, queryForm);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, list));
    }

    public ResponseDTO<String> addForm(BpmFormAddForm addForm) {
        BpmFormEntity existEntity = new BpmFormEntity();
        existEntity.setFormKey(addForm.getFormKey());
        existEntity.setDeletedFlag(Boolean.FALSE);
        if (bpmFormDao.selectOne(existEntity) != null) {
            return ResponseDTO.userErrorParam("流程表单编码已存在");
        }

        BpmFormEntity entity = SmartBeanUtil.copy(addForm, BpmFormEntity.class);
        entity.setDisabledFlag(Boolean.TRUE.equals(entity.getDisabledFlag()));
        entity.setDeletedFlag(Boolean.FALSE);
        bpmFormDao.insert(entity);
        return ResponseDTO.ok();
    }

    public ResponseDTO<String> updateForm(BpmFormUpdateForm updateForm) {
        BpmFormEntity dbEntity = bpmFormDao.selectById(updateForm.getFormId());
        if (dbEntity == null || Boolean.TRUE.equals(dbEntity.getDeletedFlag())) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        BpmFormEntity existEntity = new BpmFormEntity();
        existEntity.setFormKey(updateForm.getFormKey());
        existEntity.setDeletedFlag(Boolean.FALSE);
        BpmFormEntity duplicatedEntity = bpmFormDao.selectOne(existEntity);
        if (duplicatedEntity != null && !duplicatedEntity.getFormId().equals(updateForm.getFormId())) {
            return ResponseDTO.userErrorParam("流程表单编码已存在");
        }

        BpmFormEntity entity = SmartBeanUtil.copy(updateForm, BpmFormEntity.class);
        entity.setDisabledFlag(Boolean.TRUE.equals(entity.getDisabledFlag()));
        entity.setDeletedFlag(dbEntity.getDeletedFlag());
        bpmFormDao.updateById(entity);
        return ResponseDTO.ok();
    }

    public ResponseDTO<BpmFormVO> getFormDetail(Long formId) {
        BpmFormEntity entity = bpmFormDao.selectById(formId);
        if (entity == null || Boolean.TRUE.equals(entity.getDeletedFlag())) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return ResponseDTO.ok(SmartBeanUtil.copy(entity, BpmFormVO.class));
    }
}
