package com.hunyuan.sa.bpm.module.approvaldata.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmApprovalSubjectSnapshotDao;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmProcessWorkingDataDao;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmTaskActionEvidenceDao;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmApprovalSubjectSnapshotEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmProcessWorkingDataEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmTaskActionEvidenceEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.WorkingDataMutationCommand;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.WorkingDataMutationResult;
import com.hunyuan.sa.bpm.module.businesscontract.dao.BpmBusinessContractVersionDao;
import com.hunyuan.sa.bpm.module.businesscontract.domain.entity.BpmBusinessContractVersionEntity;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Service
public class BpmApprovalDataMutationService {

    private final BpmApprovalSubjectSnapshotDao subjectDao;
    private final BpmBusinessContractVersionDao contractDao;
    private final BpmProcessWorkingDataDao workingDataDao;
    private final BpmTaskActionEvidenceDao evidenceDao;

    public BpmApprovalDataMutationService(
            BpmApprovalSubjectSnapshotDao subjectDao,
            BpmBusinessContractVersionDao contractDao,
            BpmProcessWorkingDataDao workingDataDao,
            BpmTaskActionEvidenceDao evidenceDao
    ) {
        this.subjectDao = subjectDao;
        this.contractDao = contractDao;
        this.workingDataDao = workingDataDao;
        this.evidenceDao = evidenceDao;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkingDataMutationResult update(WorkingDataMutationCommand command) {
        if (command.approvalSubjectSnapshotId() == null) {
            throw new IllegalArgumentException("审批对象不能为空");
        }
        if (StringUtils.isBlank(command.reason())) {
            throw new IllegalArgumentException("工作数据修改原因不能为空");
        }
        BpmApprovalSubjectSnapshotEntity subject = subjectDao.selectById(command.approvalSubjectSnapshotId());
        if (subject == null) {
            throw new IllegalArgumentException("审批对象不存在");
        }
        BpmBusinessContractVersionEntity contract = contractDao.selectById(subject.getBusinessContractVersionId());
        if (contract == null) {
            throw new IllegalStateException("审批对象引用的业务契约不存在");
        }
        BpmProcessWorkingDataEntity current = workingDataDao.selectLatestBySubjectForUpdate(
                command.approvalSubjectSnapshotId()
        );
        if (current == null) {
            throw new IllegalStateException("审批对象缺少流程工作数据");
        }
        if (!Objects.equals(current.getDataVersion(), command.expectedDataVersion())) {
            throw new IllegalStateException("WORKING_DATA_VERSION_CONFLICT：流程工作数据已变化，请刷新后重试");
        }

        JSONObject contractJson = JSON.parseObject(contract.getContractJson());
        JSONObject patch = parseObject(command.patchJson(), "工作数据补丁");
        Set<String> editableFields = editableFields(contractJson.getJSONObject("changePolicy"));
        for (String key : patch.keySet()) {
            if (!editableFields.contains(key)) {
                throw new IllegalArgumentException("字段不允许在审批中修改：" + key);
            }
        }
        JSONObject before = parseObject(current.getDataJson(), "当前工作数据");
        JSONObject after = new JSONObject(true);
        after.putAll(before);
        patch.forEach(after::put);
        validateWorkingData(after, contractJson.getJSONArray("workingDataSchema"));

        boolean dataChanged = !patch.isEmpty();
        long nextVersion = dataChanged ? current.getDataVersion() + 1 : current.getDataVersion();
        String afterJson = dataChanged
                ? JSON.toJSONString(after, SerializerFeature.WriteMapNullValue)
                : current.getDataJson();
        Long processWorkingDataId = current.getProcessWorkingDataId();
        if (dataChanged) {
            BpmProcessWorkingDataEntity next = new BpmProcessWorkingDataEntity();
            next.setApprovalSubjectSnapshotId(subject.getApprovalSubjectSnapshotId());
            next.setDataVersion(nextVersion);
            next.setDataJson(afterJson);
            next.setActorEmployeeId(command.actorEmployeeId());
            next.setActorNameSnapshot(command.actorName());
            next.setChangeReason(command.reason());
            next.setPreviousDataVersion(current.getDataVersion());
            next.setDataDigest(DigestUtils.sha256Hex(afterJson));
            workingDataDao.insert(next);
            processWorkingDataId = next.getProcessWorkingDataId();
        }

        BpmTaskActionEvidenceEntity evidence = new BpmTaskActionEvidenceEntity();
        evidence.setApprovalSubjectSnapshotId(subject.getApprovalSubjectSnapshotId());
        evidence.setTaskId(command.taskId());
        evidence.setActionType(command.actionType());
        evidence.setActorEmployeeId(command.actorEmployeeId());
        evidence.setActorNameSnapshot(command.actorName());
        evidence.setActionReason(command.reason());
        evidence.setCommentText(command.comment());
        evidence.setAttachmentsJson(JSON.toJSONString(
                parseArray(command.attachmentsJson(), "动作附件")
        ));
        evidence.setBeforeWorkingDataVersion(current.getDataVersion());
        evidence.setAfterWorkingDataVersion(nextVersion);
        evidence.setChangedFieldsJson(JSON.toJSONString(patch.keySet()));
        evidence.setBeforeDataJson(current.getDataJson());
        evidence.setAfterDataJson(afterJson);
        evidence.setEvidenceDigest(DigestUtils.sha256Hex(canonicalEvidence(evidence)));
        evidenceDao.insert(evidence);

        return new WorkingDataMutationResult(
                processWorkingDataId,
                evidence.getTaskActionEvidenceId(),
                nextVersion,
                afterJson
        );
    }

    private Set<String> editableFields(JSONObject changePolicy) {
        if (changePolicy == null || !"FIELD_CONTROLLED".equals(changePolicy.getString("mode"))) {
            return Set.of();
        }
        JSONArray fields = changePolicy.getJSONArray("editableFields");
        return fields == null ? Set.of() : new LinkedHashSet<>(fields.toJavaList(String.class));
    }

    private void validateWorkingData(JSONObject data, JSONArray schema) {
        if (schema == null) {
            return;
        }
        for (int i = 0; i < schema.size(); i++) {
            JSONObject field = schema.getJSONObject(i);
            String key = field.getString("key");
            Object value = data.get(key);
            if (field.getBooleanValue("required") && value == null) {
                throw new IllegalArgumentException("流程工作数据缺少必填字段：" + key);
            }
            if (value != null && !matchesType(value, field.getString("type"))) {
                throw new IllegalArgumentException("流程工作数据字段类型不匹配：" + key);
            }
        }
    }

    private boolean matchesType(Object value, String type) {
        return switch (StringUtils.defaultString(type)) {
            case "STRING" -> value instanceof String;
            case "DECIMAL" -> value instanceof Number || canParseDecimal(value);
            case "INTEGER", "EMPLOYEE_ID" -> value instanceof Number;
            case "BOOLEAN" -> value instanceof Boolean;
            default -> false;
        };
    }

    private boolean canParseDecimal(Object value) {
        try {
            new BigDecimal(String.valueOf(value));
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private JSONObject parseObject(String json, String label) {
        try {
            JSONObject result = JSON.parseObject(StringUtils.defaultIfBlank(json, "{}"));
            return result == null ? new JSONObject(true) : result;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(label + "不是合法 JSON 对象", ex);
        }
    }

    private JSONArray parseArray(String json, String label) {
        try {
            JSONArray result = JSON.parseArray(StringUtils.defaultIfBlank(json, "[]"));
            return result == null ? new JSONArray() : result;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(label + "不是合法 JSON 数组", ex);
        }
    }

    private String canonicalEvidence(BpmTaskActionEvidenceEntity evidence) {
        JSONObject json = new JSONObject(true);
        json.put("subjectId", evidence.getApprovalSubjectSnapshotId());
        json.put("taskId", evidence.getTaskId());
        json.put("action", evidence.getActionType());
        json.put("actor", evidence.getActorEmployeeId());
        json.put("actorName", evidence.getActorNameSnapshot());
        json.put("reason", evidence.getActionReason());
        json.put("comment", evidence.getCommentText());
        json.put("signature", evidence.getSignatureJson());
        json.put("attachments", JSON.parseArray(evidence.getAttachmentsJson()));
        json.put("beforeVersion", evidence.getBeforeWorkingDataVersion());
        json.put("afterVersion", evidence.getAfterWorkingDataVersion());
        json.put("changedFields", JSON.parseArray(evidence.getChangedFieldsJson()));
        json.put("beforeData", JSON.parseObject(evidence.getBeforeDataJson()));
        json.put("afterData", JSON.parseObject(evidence.getAfterDataJson()));
        return JSON.toJSONString(json);
    }
}
