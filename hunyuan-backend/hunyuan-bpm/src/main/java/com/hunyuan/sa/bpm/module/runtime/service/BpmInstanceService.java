package com.hunyuan.sa.bpm.module.runtime.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartEnumUtil;
import com.hunyuan.sa.base.common.util.SmartPageUtil;
import com.hunyuan.sa.base.module.support.serialnumber.constant.SerialNumberIdEnum;
import com.hunyuan.sa.base.module.support.serialnumber.service.SerialNumberService;
import com.hunyuan.sa.bpm.api.identity.BpmCurrentActorProvider;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionLifecycleStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmDefinitionStartStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionDao;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionEntity;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskActionLogDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceStartForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmStartableDefinitionVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 流程实例服务。
 */
@Service
public class BpmInstanceService {

    @Resource
    private BpmDefinitionDao bpmDefinitionDao;

    @Resource
    private BpmInstanceDao bpmInstanceDao;

    @Resource
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;

    @Resource
    private BpmTaskActionLogDao bpmTaskActionLogDao;

    @Resource
    private FlowableProcessInstanceGateway flowableProcessInstanceGateway;

    @Resource
    private BpmCurrentActorProvider bpmCurrentActorProvider;

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    @Resource
    private SerialNumberService serialNumberService;

    @Resource
    private BpmTaskAssignmentResolver bpmTaskAssignmentResolver;

    @Resource
    private BpmTaskProjectionService bpmTaskProjectionService;

    public ResponseDTO<PageResult<BpmInstanceVO>> queryAdminPage(BpmInstanceQueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<BpmInstanceVO> list = bpmInstanceDao.queryPage(page, queryForm);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, list));
    }

    public ResponseDTO<PageResult<BpmInstanceVO>> queryMyInstancePage(BpmInstanceQueryForm queryForm) {
        queryForm.setStartEmployeeId(bpmCurrentActorProvider.requireCurrentEmployeeId());
        return queryAdminPage(queryForm);
    }

    public ResponseDTO<List<BpmStartableDefinitionVO>> queryStartableDefinitions() {
        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        return ResponseDTO.ok(bpmDefinitionDao.queryStartableList(employeeId));
    }

    public ResponseDTO<BpmInstanceDetailVO> getDetail(Long instanceId) {
        BpmInstanceEntity instance = bpmInstanceDao.selectById(instanceId);
        if (instance == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        BpmInstanceDetailVO detail = new BpmInstanceDetailVO();
        detail.setInstanceId(instance.getInstanceId());
        detail.setInstanceNo(instance.getInstanceNo());
        detail.setTitle(instance.getTitle());
        detail.setSummary(instance.getSummary());
        detail.setRunState(instance.getRunState());
        detail.setResultState(instance.getResultState());
        detail.setStartEmployeeNameSnapshot(instance.getStartEmployeeNameSnapshot());
        detail.setStartDepartmentNameSnapshot(instance.getStartDepartmentNameSnapshot());
        detail.setCurrentFormDataSnapshotJson(instance.getCurrentFormDataSnapshotJson());
        detail.setCurrentNodeSummaryJson(instance.getCurrentNodeSummaryJson());
        detail.setStartedAt(instance.getStartedAt());
        detail.setFinishedAt(instance.getFinishedAt());
        detail.setActionLogs(bpmTaskActionLogDao.queryByInstanceId(instanceId));
        return ResponseDTO.ok(detail);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Long> startInstance(BpmInstanceStartForm startForm) {
        Long employeeId = bpmCurrentActorProvider.requireCurrentEmployeeId();
        BpmEmployeeSnapshot employeeSnapshot = bpmOrgIdentityGateway.requireEmployee(employeeId);
        BpmDefinitionEntity definitionEntity = bpmDefinitionDao.selectById(startForm.getDefinitionId());
        if (definitionEntity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (!BpmDefinitionLifecycleStateEnum.CURRENT.equalsValue(definitionEntity.getLifecycleState())) {
            return ResponseDTO.userErrorParam("当前流程定义不是可运行的当前版本");
        }
        if (!BpmDefinitionStartStateEnum.STARTABLE.equalsValue(definitionEntity.getStartState())) {
            return ResponseDTO.userErrorParam("当前流程定义已停用，无法发起");
        }

        SerialNumberIdEnum serialNumberIdEnum = SmartEnumUtil.getEnumByValue(
                definitionEntity.getInstanceNoRuleIdSnapshot(),
                SerialNumberIdEnum.class
        );
        if (serialNumberIdEnum == null) {
            serialNumberIdEnum = SerialNumberIdEnum.ORDER;
        }

        String instanceNo = serialNumberService.generate(serialNumberIdEnum);
        List<BpmDefinitionNodeEntity> definitionNodes = bpmDefinitionNodeDao.selectList(
                Wrappers.<BpmDefinitionNodeEntity>lambdaQuery()
                        .eq(BpmDefinitionNodeEntity::getDefinitionId, definitionEntity.getDefinitionId())
                        .orderByAsc(BpmDefinitionNodeEntity::getSortOrder, BpmDefinitionNodeEntity::getDefinitionNodeId)
        );

        Map<String, Object> runtimeAssignmentVariables;
        try {
            runtimeAssignmentVariables = bpmTaskAssignmentResolver.resolve(definitionNodes, employeeSnapshot);
        } catch (IllegalArgumentException ex) {
            return ResponseDTO.userErrorParam(ex.getMessage());
        }

        String engineProcessInstanceId = flowableProcessInstanceGateway.start(
                definitionEntity.getEngineProcessDefinitionId(),
                employeeId,
                startForm.getFormDataJson(),
                runtimeAssignmentVariables
        );

        LocalDateTime now = LocalDateTime.now();
        BpmInstanceEntity entity = new BpmInstanceEntity();
        entity.setInstanceNo(instanceNo);
        entity.setDefinitionId(definitionEntity.getDefinitionId());
        entity.setEngineProcessDefinitionId(definitionEntity.getEngineProcessDefinitionId());
        entity.setEngineProcessInstanceId(engineProcessInstanceId);
        entity.setDefinitionKeySnapshot(definitionEntity.getDefinitionKey());
        entity.setDefinitionVersionSnapshot(definitionEntity.getDefinitionVersion());
        entity.setCategoryIdSnapshot(definitionEntity.getCategoryIdSnapshot());
        entity.setCategoryNameSnapshot(definitionEntity.getCategoryNameSnapshot());
        entity.setTitle(StringUtils.isBlank(startForm.getTitle()) ? definitionEntity.getDefinitionName() : startForm.getTitle());
        entity.setSummary(startForm.getSummary());
        entity.setStartEmployeeId(employeeSnapshot.employeeId());
        entity.setStartEmployeeNameSnapshot(employeeSnapshot.actualName());
        entity.setStartDepartmentIdSnapshot(employeeSnapshot.departmentId());
        entity.setStartDepartmentNameSnapshot(employeeSnapshot.departmentName());
        entity.setBusinessType(startForm.getBusinessType());
        entity.setBusinessId(startForm.getBusinessId());
        entity.setBusinessKey(startForm.getBusinessKey());
        entity.setInitialFormDataSnapshotJson(startForm.getFormDataJson());
        entity.setCurrentFormDataSnapshotJson(startForm.getFormDataJson());
        entity.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());
        entity.setActiveTaskCount(0);
        entity.setStartedAt(now);
        entity.setLastActionAt(now);
        bpmInstanceDao.insert(entity);
        bpmTaskProjectionService.syncActiveTasksForInstance(entity.getInstanceId());
        return ResponseDTO.ok(entity.getInstanceId());
    }
}
