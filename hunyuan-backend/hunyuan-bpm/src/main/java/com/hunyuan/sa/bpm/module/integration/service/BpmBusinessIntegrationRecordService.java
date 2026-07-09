package com.hunyuan.sa.bpm.module.integration.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.base.common.domain.PageParam;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCommandRecordDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCommandRecordEntity;
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmCallbackRecordQueryForm;
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmCommandRecordQueryForm;
import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCallbackRecordVO;
import com.hunyuan.sa.bpm.module.integration.domain.vo.BpmCommandRecordVO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * BPM 业务集成可靠性记录服务。
 */
@Service
public class BpmBusinessIntegrationRecordService {

    @Resource
    private BpmCallbackRecordDao bpmCallbackRecordDao;

    @Resource
    private BpmCommandRecordDao bpmCommandRecordDao;

    public ResponseDTO<PageResult<BpmCallbackRecordVO>> queryCallbackPage(BpmCallbackRecordQueryForm queryForm) {
        Page<BpmCallbackRecordEntity> page = buildPage(queryForm);
        Page<BpmCallbackRecordEntity> result = bpmCallbackRecordDao.selectPage(page, buildCallbackQuery(queryForm));
        List<BpmCallbackRecordVO> records = result.getRecords().stream()
                .map(this::toCallbackRecordVO)
                .toList();
        return ResponseDTO.ok(toPageResult(result, records));
    }

    public ResponseDTO<PageResult<BpmCommandRecordVO>> queryCommandPage(BpmCommandRecordQueryForm queryForm) {
        Page<BpmCommandRecordEntity> page = buildPage(queryForm);
        Page<BpmCommandRecordEntity> result = bpmCommandRecordDao.selectPage(page, buildCommandQuery(queryForm));
        List<BpmCommandRecordVO> records = result.getRecords().stream()
                .map(this::toCommandRecordVO)
                .toList();
        return ResponseDTO.ok(toPageResult(result, records));
    }

    public List<BpmCallbackRecordVO> queryCallbackRecordsByInstanceId(Long instanceId) {
        if (instanceId == null) {
            return List.of();
        }
        return bpmCallbackRecordDao.selectList(Wrappers.<BpmCallbackRecordEntity>lambdaQuery()
                        .eq(BpmCallbackRecordEntity::getInstanceId, instanceId)
                        .orderByAsc(BpmCallbackRecordEntity::getCreateTime, BpmCallbackRecordEntity::getCallbackRecordId))
                .stream()
                .map(this::toCallbackRecordVO)
                .toList();
    }

    public List<BpmCommandRecordVO> queryCommandRecordsByInstanceId(Long instanceId) {
        if (instanceId == null) {
            return List.of();
        }
        return bpmCommandRecordDao.selectList(Wrappers.<BpmCommandRecordEntity>lambdaQuery()
                        .eq(BpmCommandRecordEntity::getInstanceId, instanceId)
                        .orderByAsc(BpmCommandRecordEntity::getCreateTime, BpmCommandRecordEntity::getCommandRecordId))
                .stream()
                .map(this::toCommandRecordVO)
                .toList();
    }

    private <T> Page<T> buildPage(PageParam queryForm) {
        Page<T> page = new Page<>(queryForm.getPageNum(), queryForm.getPageSize());
        if (queryForm.getSearchCount() != null) {
            page.setSearchCount(queryForm.getSearchCount());
        }
        return page;
    }

    private <T> PageResult<T> toPageResult(Page<?> page, List<T> records) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setPageNum(page.getCurrent());
        pageResult.setPageSize(page.getSize());
        pageResult.setTotal(page.getTotal());
        pageResult.setPages(page.getPages());
        pageResult.setList(records);
        pageResult.setEmptyFlag(records.isEmpty());
        return pageResult;
    }

    private LambdaQueryWrapper<BpmCallbackRecordEntity> buildCallbackQuery(BpmCallbackRecordQueryForm queryForm) {
        return Wrappers.<BpmCallbackRecordEntity>lambdaQuery()
                .eq(StringUtils.isNotBlank(queryForm.getEventId()), BpmCallbackRecordEntity::getEventId, queryForm.getEventId())
                .eq(queryForm.getInstanceId() != null, BpmCallbackRecordEntity::getInstanceId, queryForm.getInstanceId())
                .eq(StringUtils.isNotBlank(queryForm.getBusinessType()), BpmCallbackRecordEntity::getBusinessType, queryForm.getBusinessType())
                .eq(queryForm.getBusinessId() != null, BpmCallbackRecordEntity::getBusinessId, queryForm.getBusinessId())
                .eq(queryForm.getCallbackStatus() != null, BpmCallbackRecordEntity::getCallbackStatus, queryForm.getCallbackStatus())
                .orderByDesc(BpmCallbackRecordEntity::getCallbackRecordId);
    }

    private LambdaQueryWrapper<BpmCommandRecordEntity> buildCommandQuery(BpmCommandRecordQueryForm queryForm) {
        return Wrappers.<BpmCommandRecordEntity>lambdaQuery()
                .eq(StringUtils.isNotBlank(queryForm.getCommandKey()), BpmCommandRecordEntity::getCommandKey, queryForm.getCommandKey())
                .eq(queryForm.getInstanceId() != null, BpmCommandRecordEntity::getInstanceId, queryForm.getInstanceId())
                .eq(StringUtils.isNotBlank(queryForm.getBusinessType()), BpmCommandRecordEntity::getBusinessType, queryForm.getBusinessType())
                .eq(queryForm.getBusinessId() != null, BpmCommandRecordEntity::getBusinessId, queryForm.getBusinessId())
                .eq(queryForm.getCommandStatus() != null, BpmCommandRecordEntity::getCommandStatus, queryForm.getCommandStatus())
                .orderByDesc(BpmCommandRecordEntity::getCommandRecordId);
    }

    private BpmCallbackRecordVO toCallbackRecordVO(BpmCallbackRecordEntity entity) {
        BpmCallbackRecordVO vo = new BpmCallbackRecordVO();
        vo.setCallbackRecordId(entity.getCallbackRecordId());
        vo.setEventId(entity.getEventId());
        vo.setInstanceId(entity.getInstanceId());
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessId(entity.getBusinessId());
        vo.setCallbackStatus(entity.getCallbackStatus());
        vo.setFailureReason(entity.getFailureReason());
        vo.setRetryCount(entity.getRetryCount());
        vo.setNextRetryAt(entity.getNextRetryAt());
        vo.setCompensatedAt(entity.getCompensatedAt());
        vo.setCompensatedBy(entity.getCompensatedBy());
        vo.setCompensationReason(entity.getCompensationReason());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private BpmCommandRecordVO toCommandRecordVO(BpmCommandRecordEntity entity) {
        BpmCommandRecordVO vo = new BpmCommandRecordVO();
        vo.setCommandRecordId(entity.getCommandRecordId());
        vo.setCommandKey(entity.getCommandKey());
        vo.setCommandType(entity.getCommandType());
        vo.setInstanceId(entity.getInstanceId());
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessId(entity.getBusinessId());
        vo.setCommandStatus(entity.getCommandStatus());
        vo.setFailureReason(entity.getFailureReason());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
