package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.common.enumeration.BpmTimeEventStatusEnum;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTimeEventDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTimeEventEntity;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 创建和维护 Hunyuan 时间事件投影。
 */
@Service
public class BpmTimeEventService {

    @Resource
    private BpmTimeEventDao bpmTimeEventDao;

    @Resource
    private BpmTaskDao bpmTaskDao;

    @Resource
    private ObjectProvider<BpmRuntimeCommandCoordinator> bpmRuntimeCommandCoordinatorProvider;

    @Resource
    private BpmInstanceDao bpmInstanceDao;

    @Resource
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;

    @Transactional(rollbackFor = Exception.class)
    public int scheduleTaskSla(
            BpmInstanceEntity instance,
            BpmDefinitionNodeEntity node,
            BpmTaskEntity task
    ) {
        JSONObject policy = readTaskSlaPolicy(node);
        if (policy == null || task.getTaskId() == null) {
            return 0;
        }
        LocalDateTime assignedAt = task.getAssignedAt() == null ? LocalDateTime.now() : task.getAssignedAt();
        int created = 0;
        JSONArray reminders = policy.getJSONArray("reminderSchedule");
        if (reminders != null) {
            for (int index = 0; index < reminders.size(); index++) {
                created += insertIfMissing(
                        instance,
                        node,
                        task,
                        "TASK:" + task.getTaskId() + ":SLA_REMINDER:" + (index + 1),
                        "SLA_REMINDER",
                        policy,
                        assignedAt.plus(Duration.parse(reminders.getString(index)))
                );
            }
        }
        LocalDateTime dueAt = assignedAt.plus(Duration.parse(policy.getString("dueAfter")));
        created += insertIfMissing(
                instance,
                node,
                task,
                "TASK:" + task.getTaskId() + ":SLA_DUE",
                "SLA_DUE",
                policy,
                dueAt
        );
        if (!dueAt.equals(task.getDueAt())) {
            BpmTaskEntity update = new BpmTaskEntity();
            update.setTaskId(task.getTaskId());
            update.setDueAt(dueAt);
            bpmTaskDao.updateById(update);
            task.setDueAt(dueAt);
        }
        return created;
    }

    private int insertIfMissing(
            BpmInstanceEntity instance,
            BpmDefinitionNodeEntity node,
            BpmTaskEntity task,
            String eventKey,
            String eventKind,
            JSONObject policy,
            LocalDateTime scheduledAt
    ) {
        Long count = bpmTimeEventDao.selectCount(Wrappers.<BpmTimeEventEntity>lambdaQuery()
                .eq(BpmTimeEventEntity::getEventKey, eventKey));
        if (count != null && count > 0) {
            return 0;
        }
        BpmTimeEventEntity event = new BpmTimeEventEntity();
        event.setEventKey(eventKey);
        event.setIdempotencyKey(eventKey);
        event.setInstanceId(instance.getInstanceId());
        event.setTaskId(task.getTaskId());
        event.setDefinitionId(instance.getDefinitionId());
        event.setDefinitionNodeId(node == null ? null : node.getDefinitionNodeId());
        event.setNodeKey(resolveAuthoredNodeKey(node, task));
        event.setEngineProcessInstanceId(instance.getEngineProcessInstanceId());
        event.setEngineExecutionId(task.getEngineExecutionId());
        event.setEngineTaskId(task.getEngineTaskId());
        event.setEventKind(eventKind);
        event.setPolicySnapshotJson(policy.toJSONString());
        event.setScheduledAt(scheduledAt);
        event.setEventStatus(BpmTimeEventStatusEnum.SCHEDULED.name());
        event.setTriggerCount(0);
        bpmTimeEventDao.insert(event);
        return 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean trigger(
            String engineProcessInstanceId,
            String nodeKey,
            String eventKind,
            String engineExecutionId,
            String engineJobId
    ) {
        BpmTimeEventEntity event = bpmTimeEventDao.selectOne(Wrappers.<BpmTimeEventEntity>lambdaQuery()
                .eq(BpmTimeEventEntity::getEngineProcessInstanceId, engineProcessInstanceId)
                .eq(BpmTimeEventEntity::getNodeKey, nodeKey)
                .eq(BpmTimeEventEntity::getEventKind, eventKind)
                .in(BpmTimeEventEntity::getEventStatus,
                        BpmTimeEventStatusEnum.SCHEDULED.name(),
                        BpmTimeEventStatusEnum.FAILED_RETRYABLE.name())
                .orderByAsc(BpmTimeEventEntity::getScheduledAt)
                .last("LIMIT 1"));
        if (event == null || BpmTimeEventStatusEnum.SUCCEEDED.name().equals(event.getEventStatus())) {
            return false;
        }
        BpmTimeEventEntity claim = new BpmTimeEventEntity();
        claim.setEventStatus(BpmTimeEventStatusEnum.TRIGGERED.name());
        claim.setTriggeredAt(LocalDateTime.now());
        claim.setTriggerCount((event.getTriggerCount() == null ? 0 : event.getTriggerCount()) + 1);
        int claimed = bpmTimeEventDao.update(claim, Wrappers.<BpmTimeEventEntity>lambdaUpdate()
                .eq(BpmTimeEventEntity::getTimeEventId, event.getTimeEventId())
                .eq(BpmTimeEventEntity::getEventStatus, event.getEventStatus()));
        if (claimed != 1) {
            return false;
        }
        event.setEventStatus(BpmTimeEventStatusEnum.TRIGGERED.name());
        event.setTriggeredAt(claim.getTriggeredAt());
        event.setTriggerCount(claim.getTriggerCount());
        event.setEngineExecutionId(engineExecutionId);
        event.setEngineJobId(engineJobId);
        bpmTimeEventDao.updateById(event);
        try {
            // 命令协调器只在事件触发时解析，避免排程投影与任务完成链形成启动期依赖环。
            bpmRuntimeCommandCoordinatorProvider.getObject().executeTimeEvent(event);
            BpmTimeEventEntity success = new BpmTimeEventEntity();
            success.setTimeEventId(event.getTimeEventId());
            success.setEventStatus(BpmTimeEventStatusEnum.SUCCEEDED.name());
            success.setCompletedAt(LocalDateTime.now());
            success.setLastError(null);
            bpmTimeEventDao.updateById(success);
            return true;
        } catch (Exception ex) {
            BpmTimeEventEntity failure = new BpmTimeEventEntity();
            failure.setTimeEventId(event.getTimeEventId());
            failure.setEventStatus(event.getTriggerCount() < 3
                    ? BpmTimeEventStatusEnum.FAILED_RETRYABLE.name()
                    : BpmTimeEventStatusEnum.FAILED_MANUAL.name());
            failure.setLastError(limitError(ex.getMessage()));
            bpmTimeEventDao.updateById(failure);
            if (event.getTriggerCount() < 3) {
                throw ex;
            }
            return false;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void scheduleDelay(Long instanceId, String engineProcessInstanceId, String executionId, String nodeKey) {
        String eventKey = "EXEC:" + executionId + ":DELAY";
        Long existing = bpmTimeEventDao.selectCount(Wrappers.<BpmTimeEventEntity>lambdaQuery()
                .eq(BpmTimeEventEntity::getEventKey, eventKey));
        if (existing != null && existing > 0) {
            return;
        }
        BpmInstanceEntity instance = bpmInstanceDao.selectById(instanceId);
        if (instance == null) {
            throw new IllegalStateException("延迟节点对应的 Hunyuan 实例不存在");
        }
        BpmDefinitionNodeEntity node = bpmDefinitionNodeDao.selectOne(
                Wrappers.<BpmDefinitionNodeEntity>lambdaQuery()
                        .eq(BpmDefinitionNodeEntity::getDefinitionId, instance.getDefinitionId())
                        .eq(BpmDefinitionNodeEntity::getNodeKey, nodeKey)
                        .last("LIMIT 1")
        );
        if (node == null) {
            throw new IllegalStateException("延迟节点冻结快照不存在");
        }
        JSONObject snapshot = JSON.parseObject(node.getCompiledNodeSnapshotJson());
        LocalDateTime scheduledAt = resolveDelayTime(snapshot, instance.getCurrentFormDataSnapshotJson());
        BpmTimeEventEntity event = new BpmTimeEventEntity();
        event.setEventKey(eventKey);
        event.setIdempotencyKey(eventKey);
        event.setInstanceId(instanceId);
        event.setDefinitionId(instance.getDefinitionId());
        event.setDefinitionNodeId(node.getDefinitionNodeId());
        event.setNodeKey(nodeKey);
        event.setEngineProcessInstanceId(engineProcessInstanceId);
        event.setEngineExecutionId(executionId);
        event.setEventKind("DELAY");
        event.setPolicySnapshotJson(snapshot.toJSONString());
        event.setScheduledAt(scheduledAt);
        event.setEventStatus(BpmTimeEventStatusEnum.SCHEDULED.name());
        event.setTriggerCount(0);
        bpmTimeEventDao.insert(event);
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeDelay(String executionId) {
        LocalDateTime now = LocalDateTime.now();
        BpmTimeEventEntity update = new BpmTimeEventEntity();
        update.setEventStatus(BpmTimeEventStatusEnum.SUCCEEDED.name());
        update.setTriggeredAt(now);
        update.setCompletedAt(now);
        update.setTriggerCount(1);
        int affected = bpmTimeEventDao.update(update, Wrappers.<BpmTimeEventEntity>lambdaUpdate()
                .eq(BpmTimeEventEntity::getEventKey, "EXEC:" + executionId + ":DELAY")
                .eq(BpmTimeEventEntity::getEventStatus, BpmTimeEventStatusEnum.SCHEDULED.name()));
        if (affected != 1) {
            throw new IllegalStateException("延迟时间事件不存在或已处理");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelPendingForInstance(Long instanceId) {
        BpmTimeEventEntity update = new BpmTimeEventEntity();
        update.setEventStatus(BpmTimeEventStatusEnum.CANCELLED.name());
        bpmTimeEventDao.update(update, Wrappers.<BpmTimeEventEntity>lambdaUpdate()
                .eq(BpmTimeEventEntity::getInstanceId, instanceId)
                .in(BpmTimeEventEntity::getEventStatus,
                        BpmTimeEventStatusEnum.SCHEDULED.name(),
                        BpmTimeEventStatusEnum.TRIGGERED.name(),
                        BpmTimeEventStatusEnum.FAILED_RETRYABLE.name()));
    }

    private LocalDateTime resolveDelayTime(JSONObject snapshot, String formDataJson) {
        String mode = snapshot.getString("mode");
        String value = snapshot.getString("value");
        if ("DURATION".equals(mode)) {
            return LocalDateTime.now().plus(Duration.parse(value));
        }
        if ("FIXED_DATETIME".equals(mode)) {
            return OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        }
        if ("FORM_DATETIME".equals(mode)) {
            JSONObject formData = JSON.parseObject(formDataJson);
            String rawValue = formData == null ? null : formData.getString(value);
            if (rawValue == null || rawValue.isBlank()) {
                throw new IllegalStateException("延迟节点表单日期为空");
            }
            try {
                return OffsetDateTime.parse(rawValue).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            } catch (Exception ignored) {
                return LocalDateTime.parse(rawValue).atZone(ZoneId.of(snapshot.getString("timezone")))
                        .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            }
        }
        throw new IllegalStateException("延迟节点模式不受支持");
    }

    private JSONObject readTaskSlaPolicy(BpmDefinitionNodeEntity node) {
        if (node == null || node.getCompiledNodeSnapshotJson() == null) {
            return null;
        }
        try {
            return JSON.parseObject(node.getCompiledNodeSnapshotJson()).getJSONObject("taskSlaPolicy");
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveAuthoredNodeKey(BpmDefinitionNodeEntity node, BpmTaskEntity task) {
        if (node != null && node.getCompiledNodeSnapshotJson() != null) {
            try {
                String authoredNodeKey = JSON.parseObject(node.getCompiledNodeSnapshotJson())
                        .getString("authoredNodeKey");
                if (authoredNodeKey != null && !authoredNodeKey.isBlank()) {
                    return authoredNodeKey;
                }
            } catch (Exception ignored) {
                // 快照损坏由发布与运行验收处理，这里保留任务 key 作为稳定兜底。
            }
        }
        return task.getTaskKey();
    }

    private String limitError(String message) {
        if (message == null) {
            return "时间事件执行失败";
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }
}
