package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.common.enumeration.BpmFormDataChangeSourceEnum;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmFormDataChangeDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmFormDataChangeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmFieldPermissionVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskFormContextVO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 审批任务表单数据的授权修改服务。
 */
@Service
public class BpmFormDataMutationService {

    @Resource
    private BpmTaskFormContextService bpmTaskFormContextService;

    @Resource
    private BpmRuntimeFormDataValidator bpmRuntimeFormDataValidator;

    @Resource
    private BpmInstanceDao bpmInstanceDao;

    @Resource
    private BpmFormDataChangeDao bpmFormDataChangeDao;

    public ResponseDTO<MutationResult> applyTaskApprovePatch(
            BpmTaskEntity task,
            BpmInstanceEntity instance,
            BpmEmployeeSnapshot actor,
            Long expectedVersion,
            String patchJson
    ) {
        long currentVersion = instance.getFormDataVersion() == null ? 1L : instance.getFormDataVersion();
        if (!Objects.equals(expectedVersion, currentVersion)) {
            return ResponseDTO.userErrorParam("FORM_DATA_VERSION_CONFLICT：审批数据已变化，请刷新后重新确认");
        }

        BpmTaskFormContextVO context = bpmTaskFormContextService.buildForEmployeeTask(task, instance);
        if (context == null) {
            return ResponseDTO.userErrorParam("当前任务字段权限快照不可用，禁止修改审批数据");
        }
        JSONObject patch;
        JSONObject currentData;
        try {
            patch = JSON.parseObject(StringUtils.defaultIfBlank(patchJson, "{}"));
            currentData = JSON.parseObject(StringUtils.defaultIfBlank(instance.getCurrentFormDataSnapshotJson(), "{}"));
        } catch (RuntimeException ex) {
            return ResponseDTO.userErrorParam("审批表单修改数据 JSON 不合法");
        }
        if (patch == null || currentData == null) {
            return ResponseDTO.userErrorParam("审批表单修改数据必须是 JSON object");
        }

        Map<String, BpmFieldPermissionVO> permissions = new LinkedHashMap<>();
        context.getPermissions().forEach(permission -> permissions.put(permission.getFieldKey(), permission));
        JSONObject beforeValues = new JSONObject(true);
        JSONObject afterValues = new JSONObject(true);
        JSONArray changedFields = new JSONArray();
        for (String fieldKey : patch.keySet()) {
            BpmFieldPermissionVO permission = permissions.get(fieldKey);
            if (permission == null || !"EDITABLE".equals(permission.getPermission())) {
                return ResponseDTO.userErrorParam("字段【" + fieldKey + "】不是当前节点可编辑字段");
            }
            Object before = currentData.get(fieldKey);
            Object after = patch.get(fieldKey);
            if (Objects.equals(before, after)) {
                continue;
            }
            beforeValues.put(fieldKey, before);
            afterValues.put(fieldKey, after);
            changedFields.add(fieldKey);
            currentData.put(fieldKey, after);
        }

        String fullSchemaJson = bpmTaskFormContextService.getPublishedFormSchemaJson(task);
        ResponseDTO<String> validateResponse = bpmRuntimeFormDataValidator.validateTaskData(
                fullSchemaJson,
                JSON.toJSONString(currentData),
                context.getPermissions()
        );
        if (!Boolean.TRUE.equals(validateResponse.getOk())) {
            return ResponseDTO.userErrorParam(validateResponse.getMsg());
        }
        if (changedFields.isEmpty()) {
            return ResponseDTO.ok(new MutationResult(false, currentVersion, JSON.toJSONString(currentData)));
        }

        long afterVersion = currentVersion + 1;
        BpmInstanceEntity update = new BpmInstanceEntity();
        update.setInstanceId(instance.getInstanceId());
        update.setCurrentFormDataSnapshotJson(JSON.toJSONString(currentData));
        update.setFormDataVersion(afterVersion);
        bpmInstanceDao.updateById(update);

        BpmFormDataChangeEntity change = new BpmFormDataChangeEntity();
        change.setInstanceId(instance.getInstanceId());
        change.setTaskId(task.getTaskId());
        change.setDefinitionNodeId(task.getDefinitionNodeId());
        change.setNodeKeySnapshot(task.getTaskKey());
        change.setChangeSource(BpmFormDataChangeSourceEnum.TASK_APPROVED.name());
        change.setActorEmployeeId(actor.employeeId());
        change.setActorNameSnapshot(actor.actualName());
        change.setBeforeVersion(currentVersion);
        change.setAfterVersion(afterVersion);
        change.setChangedFieldsJson(JSON.toJSONString(changedFields));
        change.setBeforeValuesJson(JSON.toJSONString(beforeValues));
        change.setAfterValuesJson(JSON.toJSONString(afterValues));
        bpmFormDataChangeDao.insert(change);
        return ResponseDTO.ok(new MutationResult(true, afterVersion, JSON.toJSONString(currentData)));
    }

    public record MutationResult(boolean changed, Long afterVersion, String mergedFormDataJson) {
    }
}
