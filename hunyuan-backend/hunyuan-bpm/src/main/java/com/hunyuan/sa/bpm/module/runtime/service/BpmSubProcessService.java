package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.engine.internal.FlowableProcessInstanceGateway;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmSubProcessLinkDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmSubProcessLinkEntity;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceRunStateEnum;
import com.hunyuan.sa.bpm.common.enumeration.BpmInstanceResultStateEnum;
import com.hunyuan.sa.base.module.support.serialnumber.constant.SerialNumberIdEnum;
import com.hunyuan.sa.base.module.support.serialnumber.service.SerialNumberService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class BpmSubProcessService {
    @Resource private BpmSubProcessLinkDao bpmSubProcessLinkDao;
    @Resource private BpmInstanceDao bpmInstanceDao;
    @Resource private BpmGraphRuntimeMetadataService bpmGraphRuntimeMetadataService;
    @Resource private GraphDefinitionVersionDao graphDefinitionVersionDao;
    @Resource private SerialNumberService serialNumberService;
    @Resource private FlowableProcessInstanceGateway flowableProcessInstanceGateway;

    @Transactional(rollbackFor = Exception.class)
    public Long prepare(Long parentInstanceId, String parentExecutionId, String authoredNodeId, Map<String, Object> variables) {
        return prepareChild(parentInstanceId, parentExecutionId, authoredNodeId, variables).linkId();
    }

    @Transactional(rollbackFor = Exception.class)
    public PreparedSubProcess prepareChild(Long parentInstanceId, String parentExecutionId, String authoredNodeId, Map<String, Object> variables) {
        BpmInstanceEntity parent = bpmInstanceDao.selectById(parentInstanceId);
        if (parent == null || !"GRAPH".equals(parent.getDefinitionSource())) {
            throw new IllegalArgumentException("子流程调用只允许正式 Graph 实例");
        }
        String eventKey = "SUB:" + parentInstanceId + ":" + parent.getCurrentGeneration() + ":" + authoredNodeId;
        BpmSubProcessLinkEntity existing = bpmSubProcessLinkDao.selectOne(
                Wrappers.<BpmSubProcessLinkEntity>lambdaQuery()
                        .eq(BpmSubProcessLinkEntity::getEventKey, eventKey).last("LIMIT 1"));
        if (existing != null) {
            return new PreparedSubProcess(existing.getSubProcessLinkId(), existing.getChildInstanceId());
        }
        var metadata = bpmGraphRuntimeMetadataService.requireNode(parent.getGraphDefinitionVersionId(), authoredNodeId);
        JSONObject properties = metadata.properties();
        BpmSubProcessLinkEntity link = new BpmSubProcessLinkEntity();
        link.setEventKey(eventKey);
        link.setParentInstanceId(parentInstanceId);
        link.setParentGraphDefinitionVersionId(parent.getGraphDefinitionVersionId());
        link.setParentNodeId(authoredNodeId);
        link.setParentEngineExecutionId(parentExecutionId);
        link.setCalledProcessKey(properties.getString("calledProcessKey"));
        link.setCalledDefinitionVersionId(properties.getLong("calledDefinitionVersionId"));
        JSONObject input = mapValues(properties.getJSONObject("inputMapping"), variables);
        link.setInputSnapshotJson(input.toJSONString());
        BpmInstanceEntity child = createChildInstance(parent, link.getCalledDefinitionVersionId(), input);
        link.setChildInstanceId(child.getInstanceId());
        link.setFailurePolicy(properties.getString("failurePolicy"));
        link.setCancelPropagation(properties.getString("cancelPropagation"));
        link.setLinkStatus("WAITING");
        link.setStartedAt(LocalDateTime.now());
        bpmSubProcessLinkDao.insert(link);
        return new PreparedSubProcess(link.getSubProcessLinkId(), link.getChildInstanceId());
    }

    private BpmInstanceEntity createChildInstance(BpmInstanceEntity parent, Long childVersionId, JSONObject input) {
        GraphDefinitionVersionEntity version = graphDefinitionVersionDao.selectById(childVersionId);
        if (version == null || version.getEngineProcessDefinitionId() == null) {
            throw new IllegalStateException("冻结子流程定义版本不存在或未部署");
        }
        LocalDateTime now = LocalDateTime.now();
        BpmInstanceEntity child = new BpmInstanceEntity();
        child.setInstanceNo(serialNumberService.generate(SerialNumberIdEnum.ORDER));
        child.setGraphDefinitionVersionId(childVersionId);
        child.setDefinitionSource("GRAPH");
        child.setEngineProcessDefinitionId(version.getEngineProcessDefinitionId());
        child.setDefinitionKeySnapshot(version.getProcessKey());
        child.setDefinitionVersionSnapshot(version.getDefinitionVersion());
        child.setCategoryIdSnapshot(version.getCategoryIdSnapshot());
        child.setCategoryNameSnapshot(version.getCategoryNameSnapshot());
        child.setTitle(version.getProcessNameSnapshot());
        child.setStartEmployeeId(parent.getStartEmployeeId());
        child.setStartEmployeeNameSnapshot(parent.getStartEmployeeNameSnapshot());
        child.setStartDepartmentIdSnapshot(parent.getStartDepartmentIdSnapshot());
        child.setStartDepartmentNameSnapshot(parent.getStartDepartmentNameSnapshot());
        child.setInitialFormDataSnapshotJson(input.toJSONString());
        child.setCurrentFormDataSnapshotJson(input.toJSONString());
        child.setFormDataVersion(1L);
        child.setRunState(BpmInstanceRunStateEnum.RUNNING.getValue());
        child.setActiveTaskCount(0);
        child.setCurrentGeneration(1);
        child.setStartedAt(now);
        child.setLastActionAt(now);
        bpmInstanceDao.insert(child);
        return child;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean complete(Long parentInstanceId, String authoredNodeId, Map<String, Object> variables) {
        BpmSubProcessLinkEntity link = activeLink(parentInstanceId, authoredNodeId);
        if (link == null) return false;
        BpmInstanceEntity parent = bpmInstanceDao.selectById(parentInstanceId);
        JSONObject mapping = bpmGraphRuntimeMetadataService
                .requireNode(parent.getGraphDefinitionVersionId(), authoredNodeId).properties().getJSONObject("outputMapping");
        BpmSubProcessLinkEntity update = new BpmSubProcessLinkEntity();
        update.setLinkStatus("COMPLETED");
        update.setOutputSnapshotJson(mapValues(mapping, variables).toJSONString());
        update.setCompletedAt(LocalDateTime.now());
        boolean completed = claim(link, update);
        if (completed && link.getChildInstanceId() != null) {
            BpmInstanceEntity child = new BpmInstanceEntity();
            child.setInstanceId(link.getChildInstanceId());
            child.setRunState(BpmInstanceRunStateEnum.FINISHED.getValue());
            child.setActiveTaskCount(0);
            child.setFinishedAt(update.getCompletedAt());
            child.setLastActionAt(update.getCompletedAt());
            bpmInstanceDao.updateById(child);
        }
        return completed;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean bindChildEngineInstance(Long parentInstanceId, Long childInstanceId, String childEngineInstanceId) {
        BpmSubProcessLinkEntity link = bpmSubProcessLinkDao.selectOne(
                Wrappers.<BpmSubProcessLinkEntity>lambdaQuery()
                        .eq(BpmSubProcessLinkEntity::getParentInstanceId, parentInstanceId)
                        .eq(BpmSubProcessLinkEntity::getChildInstanceId, childInstanceId)
                        .eq(BpmSubProcessLinkEntity::getLinkStatus, "WAITING")
                        .last("LIMIT 1"));
        if (link == null) return false;
        if (childEngineInstanceId.equals(link.getChildEngineProcessInstanceId())) return true;
        BpmSubProcessLinkEntity update = new BpmSubProcessLinkEntity();
        update.setChildEngineProcessInstanceId(childEngineInstanceId);
        boolean bound = bpmSubProcessLinkDao.update(update, Wrappers.<BpmSubProcessLinkEntity>lambdaUpdate()
                .eq(BpmSubProcessLinkEntity::getSubProcessLinkId, link.getSubProcessLinkId())
                .eq(BpmSubProcessLinkEntity::getLinkStatus, "WAITING")
                .and(wrapper -> wrapper.isNull(BpmSubProcessLinkEntity::getChildEngineProcessInstanceId)
                        .or().eq(BpmSubProcessLinkEntity::getChildEngineProcessInstanceId, childEngineInstanceId))) == 1;
        if (bound) {
            BpmInstanceEntity child = new BpmInstanceEntity();
            child.setInstanceId(childInstanceId);
            child.setEngineProcessInstanceId(childEngineInstanceId);
            bpmInstanceDao.updateById(child);
        }
        return bound;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean recordFailure(Long parentInstanceId, String authoredNodeId, String error) {
        BpmSubProcessLinkEntity link = activeLink(parentInstanceId, authoredNodeId);
        if (link == null) return false;
        return applyFailurePolicy(link, error);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean recordTechnicalFailure(String childEngineInstanceId, String error) {
        BpmSubProcessLinkEntity link = bpmSubProcessLinkDao.selectOne(
                Wrappers.<BpmSubProcessLinkEntity>lambdaQuery()
                        .eq(BpmSubProcessLinkEntity::getChildEngineProcessInstanceId, childEngineInstanceId)
                        .eq(BpmSubProcessLinkEntity::getLinkStatus, "WAITING")
                        .last("LIMIT 1"));
        return link != null && applyFailurePolicy(link, error);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean propagateChildRejection(Long childInstanceId, String reason) {
        BpmSubProcessLinkEntity link = bpmSubProcessLinkDao.selectOne(
                Wrappers.<BpmSubProcessLinkEntity>lambdaQuery()
                        .eq(BpmSubProcessLinkEntity::getChildInstanceId, childInstanceId)
                        .eq(BpmSubProcessLinkEntity::getLinkStatus, "WAITING")
                        .last("LIMIT 1"));
        if (link == null) return false;
        return applyFailurePolicy(link, reason);
    }

    private boolean applyFailurePolicy(BpmSubProcessLinkEntity link, String reason) {
        LocalDateTime now = LocalDateTime.now();
        BpmSubProcessLinkEntity update = new BpmSubProcessLinkEntity();
        update.setLastError(limit(reason));
        update.setCompletedAt(now);
        update.setLinkStatus(switch (link.getFailurePolicy()) {
            case "REJECT_PARENT" -> "REJECTED_PARENT";
            case "MANUAL_INTERVENTION" -> "FAILED_MANUAL";
            default -> "FAILED_PAUSED";
        });
        if (!claim(link, update)) return false;
        if ("REJECT_PARENT".equals(link.getFailurePolicy())) {
            BpmInstanceEntity parent = bpmInstanceDao.selectById(link.getParentInstanceId());
            if (parent != null && parent.getEngineProcessInstanceId() != null) {
                flowableProcessInstanceGateway.cancel(parent.getEngineProcessInstanceId(), reason);
            }
            BpmInstanceEntity parentUpdate = new BpmInstanceEntity();
            parentUpdate.setInstanceId(link.getParentInstanceId());
            parentUpdate.setRunState(BpmInstanceRunStateEnum.FINISHED.getValue());
            parentUpdate.setResultState(BpmInstanceResultStateEnum.REJECTED.getValue());
            parentUpdate.setActiveTaskCount(0);
            parentUpdate.setCurrentNodeSummaryJson(null);
            parentUpdate.setFinishedAt(update.getCompletedAt());
            parentUpdate.setLastActionAt(update.getCompletedAt());
            bpmInstanceDao.updateById(parentUpdate);
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public int cancelChildren(Long parentInstanceId, String reason) {
        BpmInstanceEntity parent = bpmInstanceDao.selectById(parentInstanceId);
        java.util.ArrayDeque<String> discoveredChildren = new java.util.ArrayDeque<>(
                parent == null || parent.getEngineProcessInstanceId() == null
                        ? java.util.List.of()
                        : flowableProcessInstanceGateway.activeChildProcessInstanceIds(parent.getEngineProcessInstanceId())
        );
        int cancelled = 0;
        for (BpmSubProcessLinkEntity link : bpmSubProcessLinkDao.selectList(
                Wrappers.<BpmSubProcessLinkEntity>lambdaQuery()
                        .eq(BpmSubProcessLinkEntity::getParentInstanceId, parentInstanceId)
                        .eq(BpmSubProcessLinkEntity::getLinkStatus, "WAITING"))) {
            if (!"CANCEL_CHILD".equals(link.getCancelPropagation())) continue;
            BpmSubProcessLinkEntity update = new BpmSubProcessLinkEntity();
            update.setLinkStatus("CANCELLED");
            update.setCancelledAt(LocalDateTime.now());
            if (claim(link, update)) {
                String childEngineId = link.getChildEngineProcessInstanceId() != null
                        ? link.getChildEngineProcessInstanceId() : discoveredChildren.pollFirst();
                if (childEngineId != null) {
                    flowableProcessInstanceGateway.cancel(childEngineId, reason);
                }
                if (link.getChildInstanceId() != null) {
                    LocalDateTime now = update.getCancelledAt();
                    BpmInstanceEntity child = new BpmInstanceEntity();
                    child.setInstanceId(link.getChildInstanceId());
                    child.setRunState(BpmInstanceRunStateEnum.CANCELLED.getValue());
                    child.setResultState(BpmInstanceResultStateEnum.CANCELLED_BY_ADMIN.getValue());
                    child.setActiveTaskCount(0);
                    child.setCurrentNodeSummaryJson(null);
                    child.setCancelReason(limit(reason));
                    child.setCancelledAt(now);
                    child.setFinishedAt(now);
                    child.setLastActionAt(now);
                    bpmInstanceDao.updateById(child);
                }
                cancelled++;
            }
        }
        return cancelled;
    }

    private BpmSubProcessLinkEntity activeLink(Long parentId, String nodeId) {
        return bpmSubProcessLinkDao.selectOne(Wrappers.<BpmSubProcessLinkEntity>lambdaQuery()
                .eq(BpmSubProcessLinkEntity::getParentInstanceId, parentId)
                .eq(BpmSubProcessLinkEntity::getParentNodeId, nodeId)
                .eq(BpmSubProcessLinkEntity::getLinkStatus, "WAITING").last("LIMIT 1"));
    }

    private boolean claim(BpmSubProcessLinkEntity link, BpmSubProcessLinkEntity update) {
        return bpmSubProcessLinkDao.update(update, Wrappers.<BpmSubProcessLinkEntity>lambdaUpdate()
                .eq(BpmSubProcessLinkEntity::getSubProcessLinkId, link.getSubProcessLinkId())
                .eq(BpmSubProcessLinkEntity::getLinkStatus, link.getLinkStatus())) == 1;
    }

    private JSONObject mapValues(JSONObject mapping, Map<String, Object> values) {
        JSONObject result = new JSONObject(true);
        if (mapping != null) mapping.forEach((target, source) -> result.put(target, values.get(String.valueOf(source))));
        return result;
    }

    private String limit(String value) {
        if (value == null) return "子流程失败";
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }

    public record PreparedSubProcess(Long linkId, Long childInstanceId) {
    }
}
