package com.hunyuan.sa.bpm.module.approvaldata.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmApprovalSubjectSnapshotDao;
import com.hunyuan.sa.bpm.module.approvaldata.dao.BpmProcessWorkingDataDao;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmApprovalSubjectSnapshotEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.entity.BpmProcessWorkingDataEntity;
import com.hunyuan.sa.bpm.module.approvaldata.domain.vo.BpmApprovalSubjectContextVO;
import com.hunyuan.sa.bpm.module.businesscontract.dao.BpmBusinessContractVersionDao;
import com.hunyuan.sa.bpm.module.businesscontract.domain.entity.BpmBusinessContractVersionEntity;
import com.hunyuan.sa.bpm.module.businesscontract.service.BusinessObjectV2DocumentMapper;
import com.hunyuan.sa.bpm.module.definition.dao.GraphDefinitionVersionDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.GraphDefinitionVersionEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmApprovalStageDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmApprovalStageEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmFieldPermissionVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BpmApprovalSubjectViewService {
    private final BusinessObjectV2DocumentMapper businessObjectMapper = new BusinessObjectV2DocumentMapper();

    private static final Map<String, Integer> SENSITIVITY_RANK = Map.of(
            "PUBLIC", 0,
            "INTERNAL", 1,
            "CONFIDENTIAL", 2,
            "RESTRICTED", 3
    );

    private final BpmApprovalSubjectSnapshotDao subjectDao;
    private final BpmProcessWorkingDataDao workingDataDao;
    private final BpmBusinessContractVersionDao contractDao;
    private final GraphDefinitionVersionDao graphVersionDao;
    private final BpmApprovalStageDao stageDao;

    public BpmApprovalSubjectViewService(
            BpmApprovalSubjectSnapshotDao subjectDao,
            BpmProcessWorkingDataDao workingDataDao,
            BpmBusinessContractVersionDao contractDao,
            GraphDefinitionVersionDao graphVersionDao,
            BpmApprovalStageDao stageDao
    ) {
        this.subjectDao = subjectDao;
        this.workingDataDao = workingDataDao;
        this.contractDao = contractDao;
        this.graphVersionDao = graphVersionDao;
        this.stageDao = stageDao;
    }

    public BpmApprovalSubjectContextVO buildForTask(BpmTaskEntity task, BpmInstanceEntity instance) {
        try {
            return buildReadyView(task, instance);
        } catch (RuntimeException ex) {
            BpmApprovalSubjectContextVO diagnostic = new BpmApprovalSubjectContextVO();
            diagnostic.setViewState("DIAGNOSTIC_ERROR");
            diagnostic.setDiagnosticMessage(ex.getMessage());
            diagnostic.setFieldPermissions(List.of());
            return diagnostic;
        }
    }

    private BpmApprovalSubjectContextVO buildReadyView(BpmTaskEntity task, BpmInstanceEntity instance) {
        if (instance == null || instance.getApprovalSubjectSnapshotId() == null
                || instance.getProcessWorkingDataId() == null || instance.getGraphDefinitionVersionId() == null) {
            throw new IllegalStateException("任务缺少 M3 审批对象运行引用");
        }
        BpmApprovalSubjectSnapshotEntity subject = require(
                subjectDao.selectById(instance.getApprovalSubjectSnapshotId()), "审批对象快照不存在"
        );
        BpmProcessWorkingDataEntity working = require(
                workingDataDao.selectById(instance.getProcessWorkingDataId()), "流程工作数据版本不存在"
        );
        BpmBusinessContractVersionEntity contract = require(
                contractDao.selectById(subject.getBusinessContractVersionId()), "业务契约版本不存在"
        );
        GraphDefinitionVersionEntity graph = require(
                graphVersionDao.selectById(instance.getGraphDefinitionVersionId()), "Graph 定义版本不存在"
        );
        String authoredNodeId = task.getTaskKey();
        if (task.getApprovalStageId() != null) {
            BpmApprovalStageEntity stage = require(stageDao.selectById(task.getApprovalStageId()), "审批阶段不存在");
            authoredNodeId = stage.getAuthoredNodeId();
        }
        JSONObject properties = nodeProperties(graph.getGraphSnapshotJson(), authoredNodeId);
        int clearance = sensitivityRank(properties.getString("maxSensitivity"), "INTERNAL");
        Map<String, String> configuredPermissions = fieldPermissions(properties.getJSONArray("fieldPermissions"));
        JSONObject contractJson = JSON.parseObject(contract.getContractJson());
        JSONObject subjectFields = JSON.parseObject(subject.getFieldsJson());
        JSONObject workingFields = JSON.parseObject(working.getDataJson());
        JSONObject visibleSubjectFields = new JSONObject(true);
        JSONObject visibleWorkingFields = new JSONObject(true);
        List<BpmFieldPermissionVO> visiblePermissions = new ArrayList<>();
        appendVisibleFields(
                contractJson.getJSONArray("fieldSchema"), subjectFields, configuredPermissions,
                clearance, visibleSubjectFields, visiblePermissions
        );
        appendVisibleFields(
                contractJson.getJSONArray("workingDataSchema"), workingFields, configuredPermissions,
                clearance, visibleWorkingFields, visiblePermissions
        );

        BpmApprovalSubjectContextVO context = new BpmApprovalSubjectContextVO();
        context.setViewState("READY");
        context.setApprovalSubjectSnapshotId(subject.getApprovalSubjectSnapshotId());
        context.setTitle(subject.getTitle());
        context.setSummary(subject.getSummary());
        context.setFieldsJson(JSON.toJSONString(visibleSubjectFields));
        context.setLineItemsJson(subject.getLineItemsJson());
        context.setAttachmentsJson(subject.getAttachmentsJson());
        context.setWorkingDataJson(JSON.toJSONString(visibleWorkingFields));
        context.setWorkingDataVersion(working.getDataVersion());
        context.setFieldPermissions(visiblePermissions);
        if (Integer.valueOf(2).equals(contract.getSchemaVersion())) {
            context.setBusinessObjectConfiguration(businessObjectMapper.restore(
                    contract.getContractKey(), contract.getObjectName(), contract.getDescription(),
                    contract.getCatalogRevision() == null ? 0L : contract.getCatalogRevision(),
                    contract.getContractJson()
            ));
        }
        return context;
    }

    private void appendVisibleFields(
            JSONArray schema,
            JSONObject data,
            Map<String, String> configuredPermissions,
            int clearance,
            JSONObject visibleData,
            List<BpmFieldPermissionVO> visiblePermissions
    ) {
        if (schema == null || data == null) {
            return;
        }
        for (int index = 0; index < schema.size(); index++) {
            JSONObject definition = schema.getJSONObject(index);
            String key = definition.getString("key");
            String permission = configuredPermissions.getOrDefault(key, "READONLY");
            if ("HIDDEN".equals(permission)
                    || sensitivityRank(definition.getString("sensitivity"), "INTERNAL") > clearance) {
                continue;
            }
            if (data.containsKey(key)) {
                visibleData.put(key, data.get(key));
            }
            BpmFieldPermissionVO fieldPermission = new BpmFieldPermissionVO();
            fieldPermission.setFieldKey(key);
            fieldPermission.setPermission(permission);
            fieldPermission.setRequired(definition.getBooleanValue("required"));
            visiblePermissions.add(fieldPermission);
        }
    }

    private JSONObject nodeProperties(String graphJson, String authoredNodeId) {
        JSONObject graph = JSON.parseObject(graphJson);
        JSONArray nodes = graph == null ? null : graph.getJSONArray("nodes");
        if (nodes == null) {
            throw new IllegalStateException("Graph 快照缺少节点");
        }
        for (int index = 0; index < nodes.size(); index++) {
            JSONObject node = nodes.getJSONObject(index);
            if (node != null && authoredNodeId != null && authoredNodeId.equals(node.getString("nodeId"))) {
                JSONObject properties = node.getJSONObject("properties");
                return properties == null ? new JSONObject(true) : properties;
            }
        }
        throw new IllegalStateException("Graph 快照中找不到当前审批节点");
    }

    private Map<String, String> fieldPermissions(JSONArray configured) {
        Map<String, String> result = new LinkedHashMap<>();
        if (configured == null) {
            return result;
        }
        for (int index = 0; index < configured.size(); index++) {
            JSONObject item = configured.getJSONObject(index);
            String key = item == null ? null : item.getString("fieldKey");
            String permission = item == null ? null : item.getString("permission");
            if (key == null || !("READONLY".equals(permission)
                    || "EDITABLE".equals(permission) || "HIDDEN".equals(permission))) {
                throw new IllegalStateException("Graph 节点字段权限配置非法");
            }
            result.put(key, permission);
        }
        return result;
    }

    private int sensitivityRank(String value, String fallback) {
        Integer rank = SENSITIVITY_RANK.get(value == null ? fallback : value);
        if (rank == null) {
            throw new IllegalStateException("业务契约敏感级别非法：" + value);
        }
        return rank;
    }

    private <T> T require(T value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return value;
    }
}
