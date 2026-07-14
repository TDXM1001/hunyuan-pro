package com.hunyuan.sa.bpm.module.approvaldata.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmApprovalSubjectSnapshotDao;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmProcessWorkingDataDao;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmRoutingFactSnapshotDao;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmApprovalSubjectSnapshotEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmProcessWorkingDataEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmRoutingFactSnapshotEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.ApprovalSubjectCreateCommand;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.ApprovalSubjectCreationResult;
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
import java.util.regex.Pattern;

@Service
public class BpmApprovalSubjectService {

    private final BpmBusinessContractVersionDao contractDao;
    private final BpmApprovalSubjectSnapshotDao subjectDao;
    private final BpmRoutingFactSnapshotDao routingFactDao;
    private final BpmProcessWorkingDataDao workingDataDao;

    public BpmApprovalSubjectService(
            BpmBusinessContractVersionDao contractDao,
            BpmApprovalSubjectSnapshotDao subjectDao,
            BpmRoutingFactSnapshotDao routingFactDao,
            BpmProcessWorkingDataDao workingDataDao
    ) {
        this.contractDao = contractDao;
        this.subjectDao = subjectDao;
        this.routingFactDao = routingFactDao;
        this.workingDataDao = workingDataDao;
    }

    @Transactional(rollbackFor = Exception.class)
    public ApprovalSubjectCreationResult create(ApprovalSubjectCreateCommand command) {
        requireText(command.contractKey(), "业务契约不能为空");
        requireText(command.sourceSystem(), "来源系统不能为空");
        requireText(command.businessType(), "业务类型不能为空");
        requireText(command.businessKey(), "业务键不能为空");
        requireText(command.title(), "审批标题不能为空");

        BpmBusinessContractVersionEntity contract = contractDao.selectActiveByKeyAndVersion(
                command.contractKey(), command.contractVersion()
        );
        if (contract == null) {
            throw new IllegalArgumentException("业务契约未启用或不存在");
        }
        JSONObject contractJson = JSON.parseObject(contract.getContractJson());
        requireEqual(contractJson.getString("sourceSystem"), command.sourceSystem(), "来源系统与业务契约不匹配");
        requireEqual(contractJson.getString("businessType"), command.businessType(), "业务类型与业务契约不匹配");
        validateBusinessKey(contractJson.getJSONObject("businessKeyRule"), command.businessKey());
        rejectDuplicateBusinessObject(command);

        JSONObject fields = parseObject(command.fieldsJson(), "审批字段");
        JSONArray lineItems = parseArray(command.lineItemsJson(), "审批明细");
        JSONArray attachments = parseArray(command.attachmentsJson(), "审批附件");
        JSONObject routingFacts = parseObject(command.routingFactsJson(), "路由事实");
        JSONObject workingData = parseObject(command.workingDataJson(), "流程工作数据");
        validateData(fields, contractJson.getJSONArray("fieldSchema"), "审批字段");
        validateData(workingData, contractJson.getJSONArray("workingDataSchema"), "流程工作数据");
        validateLineItems(lineItems, contractJson.getJSONObject("lineItemSchema"), contract.getSchemaVersion());
        validateAttachments(attachments, contractJson.getJSONObject("attachmentRules"));
        JSONObject frozenRoutingFacts = validateAndTrimRoutingFacts(
                routingFacts, contractJson.getJSONArray("routingFacts")
        );

        BpmApprovalSubjectSnapshotEntity subject = new BpmApprovalSubjectSnapshotEntity();
        subject.setBusinessContractVersionId(contract.getBusinessContractVersionId());
        subject.setSourceSystem(command.sourceSystem());
        subject.setBusinessType(command.businessType());
        subject.setBusinessKey(command.businessKey());
        subject.setSubjectVersion(1L);
        subject.setTitle(command.title());
        subject.setSummary(command.summary());
        subject.setFieldsJson(JSON.toJSONString(fields));
        subject.setLineItemsJson(JSON.toJSONString(lineItems));
        subject.setAttachmentsJson(JSON.toJSONString(attachments));
        subject.setSubmitterEmployeeId(command.submitterEmployeeId());
        subject.setSubmitterNameSnapshot(command.submitterName());
        subject.setSnapshotState("ACTIVE");
        subjectDao.insert(subject);

        Set<String> allowedFactKeys = new LinkedHashSet<>(frozenRoutingFacts.keySet());
        BpmRoutingFactSnapshotEntity routing = new BpmRoutingFactSnapshotEntity();
        routing.setApprovalSubjectSnapshotId(subject.getApprovalSubjectSnapshotId());
        routing.setBusinessContractVersionId(contract.getBusinessContractVersionId());
        routing.setRoutingFactVersion(1L);
        routing.setFactsJson(JSON.toJSONString(frozenRoutingFacts));
        routing.setAllowedFactKeysJson(JSON.toJSONString(allowedFactKeys));
        routing.setSnapshotDigest(DigestUtils.sha256Hex(routing.getFactsJson()));
        routingFactDao.insert(routing);

        BpmProcessWorkingDataEntity working = new BpmProcessWorkingDataEntity();
        working.setApprovalSubjectSnapshotId(subject.getApprovalSubjectSnapshotId());
        working.setDataVersion(1L);
        working.setDataJson(JSON.toJSONString(workingData, SerializerFeature.WriteMapNullValue));
        working.setActorEmployeeId(command.submitterEmployeeId());
        working.setActorNameSnapshot(command.submitterName());
        working.setChangeReason("SUBMITTED");
        working.setDataDigest(DigestUtils.sha256Hex(working.getDataJson()));
        workingDataDao.insert(working);

        return new ApprovalSubjectCreationResult(
                subject.getApprovalSubjectSnapshotId(),
                routing.getRoutingFactSnapshotId(),
                working.getProcessWorkingDataId(),
                1L,
                1L,
                1L
        );
    }

    private void rejectDuplicateBusinessObject(ApprovalSubjectCreateCommand command) {
        Long count = subjectDao.selectCount(Wrappers.<BpmApprovalSubjectSnapshotEntity>lambdaQuery()
                .eq(BpmApprovalSubjectSnapshotEntity::getSourceSystem, command.sourceSystem())
                .eq(BpmApprovalSubjectSnapshotEntity::getBusinessType, command.businessType())
                .eq(BpmApprovalSubjectSnapshotEntity::getBusinessKey, command.businessKey()));
        if (count != null && count > 0) {
            throw new IllegalStateException("业务对象已存在，不能重复创建审批对象");
        }
    }

    private JSONObject validateAndTrimRoutingFacts(JSONObject input, JSONArray schema) {
        JSONObject result = new JSONObject(true);
        if (schema == null) {
            return result;
        }
        for (int i = 0; i < schema.size(); i++) {
            JSONObject field = schema.getJSONObject(i);
            String key = field.getString("key");
            Object value = input.get(key);
            if (field.getBooleanValue("required") && value == null) {
                throw new IllegalArgumentException("缺少必需路由事实：" + key);
            }
            if (value == null || !field.getBooleanValue("candidateUsable")) {
                continue;
            }
            validateType(key, value, field.getString("type"), "路由事实");
            result.put(key, value);
        }
        return result;
    }

    private void validateData(JSONObject data, JSONArray schema, String label) {
        Set<String> declaredKeys = new LinkedHashSet<>();
        if (schema == null) {
            rejectUndeclaredKeys(data, declaredKeys, label);
            return;
        }
        for (int i = 0; i < schema.size(); i++) {
            JSONObject field = schema.getJSONObject(i);
            String key = field.getString("key");
            declaredKeys.add(key);
            Object value = data.get(key);
            if (field.getBooleanValue("required") && value == null) {
                throw new IllegalArgumentException(label + "缺少必填字段：" + key);
            }
            if (value != null) {
                validateType(key, value, field.getString("type"), label);
            }
        }
        rejectUndeclaredKeys(data, declaredKeys, label);
    }

    private void rejectUndeclaredKeys(JSONObject data, Set<String> declaredKeys, String label) {
        for (String key : data.keySet()) {
            if (!declaredKeys.contains(key)) {
                throw new IllegalArgumentException(label + "字段未在业务契约中声明：" + key);
            }
        }
    }

    private void validateType(String key, Object value, String type, String label) {
        boolean valid = switch (StringUtils.defaultString(type)) {
            case "STRING", "DATE", "DATETIME" -> value instanceof String;
            case "DECIMAL" -> value instanceof Number || canParseDecimal(value);
            case "INTEGER", "EMPLOYEE_ID" -> value instanceof Number;
            case "BOOLEAN" -> value instanceof Boolean;
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException(label + "字段类型不匹配：" + key);
        }
    }

    private boolean canParseDecimal(Object value) {
        try {
            new BigDecimal(String.valueOf(value));
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void validateAttachments(JSONArray attachments, JSONObject rules) {
        if (rules == null) {
            return;
        }
        if (rules.getBooleanValue("required") && attachments.isEmpty()) {
            throw new IllegalArgumentException("审批附件不能为空");
        }
        if (rules.getInteger("maxCount") != null && attachments.size() > rules.getIntValue("maxCount")) {
            throw new IllegalArgumentException("审批附件数量超过业务契约限制");
        }
        Set<String> allowed = new LinkedHashSet<>();
        JSONArray allowedExtensions = rules.getJSONArray("allowedExtensions");
        if (allowedExtensions != null) {
            allowedExtensions.forEach(value -> allowed.add(String.valueOf(value).toLowerCase(java.util.Locale.ROOT)));
        }
        Integer maxSizeMb = rules.getInteger("maxSizeMb");
        for (int index = 0; index < attachments.size(); index++) {
            JSONObject attachment = attachments.getJSONObject(index);
            String fileName = attachment == null ? null : attachment.getString("fileName");
            String extension = fileName == null || !fileName.contains(".") ? ""
                    : fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT);
            if (!allowed.isEmpty() && !allowed.contains(extension)) {
                throw new IllegalArgumentException("附件类型不允许：" + fileName);
            }
            if (maxSizeMb != null && attachment != null && attachment.getBigDecimal("sizeMb") != null
                    && attachment.getBigDecimal("sizeMb").compareTo(BigDecimal.valueOf(maxSizeMb)) > 0) {
                throw new IllegalArgumentException("附件大小超过业务契约限制：" + fileName);
            }
        }
    }

    private void validateLineItems(JSONArray lineItems, JSONObject schema, Integer schemaVersion) {
        if (schema == null) {
            if (Integer.valueOf(2).equals(schemaVersion) && !lineItems.isEmpty()) {
                throw new IllegalArgumentException("业务契约未声明审批明细");
            }
            return;
        }
        int minRows = schema.getIntValue("minRows");
        int maxRows = schema.getIntValue("maxRows");
        if (lineItems.size() < minRows || (maxRows > 0 && lineItems.size() > maxRows)) {
            throw new IllegalArgumentException("审批明细行数不符合业务契约限制");
        }
        JSONArray fieldSchema = schema.getJSONArray("fields");
        for (int index = 0; index < lineItems.size(); index++) {
            validateData(lineItems.getJSONObject(index), fieldSchema, "审批明细");
        }
    }

    private void validateBusinessKey(JSONObject rule, String businessKey) {
        if (rule == null || StringUtils.isBlank(rule.getString("pattern"))) {
            return;
        }
        if (!Pattern.matches(rule.getString("pattern"), businessKey)) {
            throw new IllegalArgumentException("业务键不符合业务契约规则");
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

    private void requireText(String value, String message) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireEqual(String expected, String actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new IllegalArgumentException(message);
        }
    }
}
