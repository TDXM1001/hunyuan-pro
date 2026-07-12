package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTimeEventEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 时间与外部事件进入 Hunyuan 运行时的统一命令边界。
 */
@Service
public class BpmRuntimeCommandCoordinator {

    @Resource
    private BpmTaskDao bpmTaskDao;

    @Resource
    private BpmOrgIdentityGateway bpmOrgIdentityGateway;

    @Resource
    private BpmNotificationListenerService bpmNotificationListenerService;

    @Resource
    private BpmTaskService bpmTaskService;

    @Resource
    private BpmExternalWaitService bpmExternalWaitService;

    public void executeTimeEvent(BpmTimeEventEntity event) {
        if ("EXTERNAL_TIMEOUT".equals(event.getEventKind())) {
            JSONObject policy = JSONObject.parseObject(event.getPolicySnapshotJson());
            if (!bpmExternalWaitService.markTimedOut(policy.getLong("externalWaitId"))) {
                throw new IllegalStateException("外部等待已由回调或取消动作处理");
            }
            return;
        }
        if ("SLA_REMINDER".equals(event.getEventKind())) {
            dispatchSlaNotification(event, "流程待办即将到期");
            return;
        }
        if (!"SLA_DUE".equals(event.getEventKind())) {
            return;
        }
        JSONObject policy = JSONObject.parseObject(event.getPolicySnapshotJson());
        String action = policy.getString("timeoutAction");
        if ("NONE".equals(action)) {
            return;
        }
        if ("REMIND_ONLY".equals(action)) {
            dispatchSlaNotification(event, "流程待办已到期");
            return;
        }
        ResponseDTO<String> response = bpmTaskService.executeSystemTimeoutAction(
                event.getTaskId(),
                action,
                policy.getString("systemActionComment"),
                policy.getLong("adminEmployeeId")
        );
        if (!Boolean.TRUE.equals(response.getOk())) {
            throw new IllegalStateException(response.getMsg());
        }
    }

    private void dispatchSlaNotification(BpmTimeEventEntity event, String title) {
        BpmTaskEntity task = bpmTaskDao.selectById(event.getTaskId());
        if (task == null || task.getAssigneeEmployeeId() == null) {
            throw new IllegalStateException("SLA 事件对应的活动任务不存在");
        }
        BpmEmployeeSnapshot receiver = bpmOrgIdentityGateway.requireEmployee(task.getAssigneeEmployeeId());
        JSONObject snapshot = new JSONObject();
        snapshot.put("employeeId", receiver.employeeId());
        snapshot.put("actualName", receiver.actualName());
        snapshot.put("departmentId", receiver.departmentId());
        snapshot.put("departmentName", receiver.departmentName());
        snapshot.put("phone", receiver.phone());
        snapshot.put("email", receiver.email());
        bpmNotificationListenerService.dispatch(new BpmNotificationCommand(
                List.of("MESSAGE"),
                event.getInstanceId(),
                task.getTaskId(),
                event.getDefinitionId(),
                event.getDefinitionNodeId(),
                event.getEventKey(),
                receiver.employeeId(),
                snapshot.toJSONString(),
                receiver.phone(),
                receiver.email() == null ? List.of() : List.of(receiver.email()),
                title,
                title,
                title + "：" + task.getInstanceTitle() + " / " + task.getTaskName(),
                "bpm_sla_reminder"
        ));
    }
}
