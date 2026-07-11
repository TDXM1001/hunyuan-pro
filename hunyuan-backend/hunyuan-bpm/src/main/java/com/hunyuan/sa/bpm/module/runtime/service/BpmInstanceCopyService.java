package com.hunyuan.sa.bpm.module.runtime.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmCopyReadStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmCopyTypeEnum;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceCopyDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceCopyEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceCopyQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceCopyVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;

/**
 * 流程抄送服务。
 */
@Service
public class BpmInstanceCopyService {

    @Resource
    private BpmInstanceCopyDao bpmInstanceCopyDao;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    public ResponseDTO<String> createManualCopies(
            BpmTaskEntity taskEntity,
            Collection<Long> targetEmployeeIds,
            String reasonSnapshot,
            BpmCopyTypeEnum copyTypeEnum
    ) {
        if (targetEmployeeIds == null || targetEmployeeIds.isEmpty()) {
            return ResponseDTO.ok();
        }

        Long currentEmployeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        LinkedHashSet<Long> targetIds = new LinkedHashSet<>();
        for (Long targetEmployeeId : targetEmployeeIds) {
            if (targetEmployeeId != null && !targetEmployeeId.equals(currentEmployeeId)) {
                targetIds.add(targetEmployeeId);
            }
        }
        if (targetIds.isEmpty()) {
            return ResponseDTO.ok();
        }

        LocalDateTime now = LocalDateTime.now();
        for (Long targetId : targetIds) {
            BpmEmployeeSnapshot snapshot = bpmOrgIdentityGateway.requireEmployee(targetId);
            BpmInstanceCopyEntity entity = new BpmInstanceCopyEntity();
            entity.setInstanceId(taskEntity.getInstanceId());
            entity.setDefinitionId(taskEntity.getDefinitionId());
            entity.setDefinitionNodeId(taskEntity.getDefinitionNodeId());
            entity.setEngineProcessInstanceId(taskEntity.getEngineProcessInstanceId());
            entity.setSourceNodeKey(taskEntity.getTaskKey());
            entity.setSourceNodeName(taskEntity.getTaskName());
            entity.setTargetEmployeeId(snapshot.employeeId());
            entity.setTargetNameSnapshot(snapshot.actualName());
            entity.setCopyType(copyTypeEnum.name());
            entity.setReadState(BpmCopyReadStateEnum.UNREAD.getValue());
            entity.setReasonSnapshot(reasonSnapshot);
            entity.setSentAt(now);
            bpmInstanceCopyDao.insert(entity);
        }
        return ResponseDTO.ok();
    }

    public ResponseDTO<String> createDesignCopies(
            BpmInstanceEntity instance,
            BpmDefinitionNodeEntity node,
            String engineProcessInstanceId,
            Collection<Long> targetEmployeeIds
    ) {
        if (targetEmployeeIds == null || targetEmployeeIds.isEmpty()) {
            return ResponseDTO.userErrorParam("设计时抄送节点未解析到接收人");
        }
        LinkedHashSet<Long> targetIds = new LinkedHashSet<>(targetEmployeeIds);
        LocalDateTime now = LocalDateTime.now();
        String sourceEventKey = "COPY:" + engineProcessInstanceId + ":" + node.getNodeKey();
        for (Long targetId : targetIds) {
            BpmEmployeeSnapshot snapshot = bpmOrgIdentityGateway.requireEmployee(targetId);
            BpmInstanceCopyEntity entity = new BpmInstanceCopyEntity();
            entity.setInstanceId(instance.getInstanceId());
            entity.setDefinitionId(instance.getDefinitionId());
            entity.setDefinitionNodeId(node.getDefinitionNodeId());
            entity.setEngineProcessInstanceId(engineProcessInstanceId);
            entity.setSourceNodeKey(node.getNodeKey());
            entity.setSourceNodeName(node.getNodeNameSnapshot());
            entity.setSourceEventKey(sourceEventKey);
            entity.setTargetEmployeeId(snapshot.employeeId());
            entity.setTargetNameSnapshot(snapshot.actualName());
            entity.setCopyType(BpmCopyTypeEnum.DESIGN_NODE_COPY.name());
            entity.setReadState(BpmCopyReadStateEnum.UNREAD.getValue());
            entity.setReasonSnapshot("流程进入设计时抄送节点");
            entity.setSentAt(now);
            try {
                bpmInstanceCopyDao.insert(entity);
            } catch (DuplicateKeyException ignored) {
                // Flowable 重试同一 service task 时复用唯一事实，不重复抄送。
            }
        }
        return ResponseDTO.ok();
    }

    public ResponseDTO<PageResult<BpmInstanceCopyVO>> queryMyCopyPage(BpmInstanceCopyQueryForm queryForm) {
        queryForm.setTargetEmployeeId(bpmCurrentActorProvider.requireCurrentEmployeeId());
        Page<?> page = new Page<>(
                queryForm.getPageNum(),
                queryForm.getPageSize(),
                queryForm.getSearchCount() == null || queryForm.getSearchCount()
        );
        List<BpmInstanceCopyVO> list = bpmInstanceCopyDao.queryMyCopyPage(page, queryForm);
        PageResult<BpmInstanceCopyVO> pageResult = new PageResult<>();
        pageResult.setPageNum(page.getCurrent());
        pageResult.setPageSize(page.getSize());
        pageResult.setTotal(page.getTotal());
        pageResult.setPages(page.getPages());
        pageResult.setList(list);
        pageResult.setEmptyFlag(list == null || list.isEmpty());
        return ResponseDTO.ok(pageResult);
    }

    public ResponseDTO<String> markRead(Long copyId) {
        Long currentEmployeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmInstanceCopyEntity updateEntity = new BpmInstanceCopyEntity();
        updateEntity.setReadState(BpmCopyReadStateEnum.READ.getValue());
        updateEntity.setReadAt(LocalDateTime.now());
        bpmInstanceCopyDao.update(updateEntity, Wrappers.<BpmInstanceCopyEntity>lambdaUpdate()
                .eq(BpmInstanceCopyEntity::getCopyId, copyId)
                .eq(BpmInstanceCopyEntity::getTargetEmployeeId, currentEmployeeId)
                .eq(BpmInstanceCopyEntity::getReadState, BpmCopyReadStateEnum.UNREAD.getValue()));
        return ResponseDTO.ok();
    }
}
