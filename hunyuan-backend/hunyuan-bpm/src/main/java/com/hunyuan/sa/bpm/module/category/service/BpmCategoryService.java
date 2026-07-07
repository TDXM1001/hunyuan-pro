package com.hunyuan.sa.bpm.module.category.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.common.util.SmartPageUtil;
import com.hunyuan.sa.bpm.module.category.dao.BpmCategoryDao;
import com.hunyuan.sa.bpm.module.category.domain.entity.BpmCategoryEntity;
import com.hunyuan.sa.bpm.module.category.domain.form.BpmCategoryAddForm;
import com.hunyuan.sa.bpm.module.category.domain.form.BpmCategoryQueryForm;
import com.hunyuan.sa.bpm.module.category.domain.form.BpmCategoryUpdateForm;
import com.hunyuan.sa.bpm.module.category.domain.vo.BpmCategoryVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程分类服务。
 */
@Service
public class BpmCategoryService {

    @Resource
    private BpmCategoryDao bpmCategoryDao;

    public ResponseDTO<PageResult<BpmCategoryVO>> queryCategory(BpmCategoryQueryForm queryForm) {
        queryForm.setDeletedFlag(Boolean.FALSE);
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<BpmCategoryVO> list = bpmCategoryDao.queryPage(page, queryForm);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, list));
    }

    public ResponseDTO<String> addCategory(BpmCategoryAddForm addForm) {
        BpmCategoryEntity existEntity = new BpmCategoryEntity();
        existEntity.setCategoryCode(addForm.getCategoryCode());
        existEntity.setDeletedFlag(Boolean.FALSE);
        if (bpmCategoryDao.selectOne(existEntity) != null) {
            return ResponseDTO.userErrorParam("流程分类编码已存在");
        }

        BpmCategoryEntity entity = SmartBeanUtil.copy(addForm, BpmCategoryEntity.class);
        entity.setSort(entity.getSort() == null ? 0 : entity.getSort());
        entity.setDisabledFlag(Boolean.TRUE.equals(entity.getDisabledFlag()));
        entity.setDeletedFlag(Boolean.FALSE);
        bpmCategoryDao.insert(entity);
        return ResponseDTO.ok();
    }

    public ResponseDTO<String> updateCategory(BpmCategoryUpdateForm updateForm) {
        BpmCategoryEntity dbEntity = bpmCategoryDao.selectById(updateForm.getCategoryId());
        if (dbEntity == null || Boolean.TRUE.equals(dbEntity.getDeletedFlag())) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        BpmCategoryEntity existEntity = new BpmCategoryEntity();
        existEntity.setCategoryCode(updateForm.getCategoryCode());
        existEntity.setDeletedFlag(Boolean.FALSE);
        BpmCategoryEntity duplicatedEntity = bpmCategoryDao.selectOne(existEntity);
        if (duplicatedEntity != null && !duplicatedEntity.getCategoryId().equals(updateForm.getCategoryId())) {
            return ResponseDTO.userErrorParam("流程分类编码已存在");
        }

        BpmCategoryEntity entity = SmartBeanUtil.copy(updateForm, BpmCategoryEntity.class);
        entity.setSort(entity.getSort() == null ? 0 : entity.getSort());
        entity.setDisabledFlag(Boolean.TRUE.equals(entity.getDisabledFlag()));
        entity.setDeletedFlag(dbEntity.getDeletedFlag());
        bpmCategoryDao.updateById(entity);
        return ResponseDTO.ok();
    }

    public ResponseDTO<BpmCategoryVO> getCategoryDetail(Long categoryId) {
        BpmCategoryEntity entity = bpmCategoryDao.selectById(categoryId);
        if (entity == null || Boolean.TRUE.equals(entity.getDeletedFlag())) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return ResponseDTO.ok(SmartBeanUtil.copy(entity, BpmCategoryVO.class));
    }
}
