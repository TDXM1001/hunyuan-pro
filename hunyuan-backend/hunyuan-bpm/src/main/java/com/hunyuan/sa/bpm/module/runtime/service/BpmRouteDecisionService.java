package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.exception.BusinessException;
import com.hunyuan.sa.bpm.engine.ast.RouteCondition;
import com.hunyuan.sa.bpm.engine.route.BpmRouteExpressionContext;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.RoutingDataSnapshot;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmApprovalRuntimeDataService;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmRouteDecisionDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmRouteDecisionEntity;
import jakarta.annotation.Resource;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 在 Hunyuan 事务内计算、冻结并复用路由决定。
 */
@Service
public class BpmRouteDecisionService {

    @Resource
    private BpmInstanceDao bpmInstanceDao;

    @Resource
    private BpmDefinitionNodeDao bpmDefinitionNodeDao;

    @Resource
    private BpmRouteDecisionDao bpmRouteDecisionDao;

    @Resource
    private BpmRouteConditionEvaluator conditionEvaluator;

    @Resource
    private BpmApprovalRuntimeDataService bpmApprovalRuntimeDataService;

    @Transactional(rollbackFor = Exception.class)
    public BpmRouteDecisionResult evaluateAndRecord(BpmRouteDecisionCommand command) {
        BpmRouteDecisionEntity existing = findExisting(command);
        if (existing != null) {
            return toResult(existing);
        }

        BpmInstanceEntity instance = bpmInstanceDao.selectByIdForUpdate(command.instanceId());
        if (instance == null) {
            throw new BusinessException(UserErrorCode.DATA_NOT_EXIST);
        }
        existing = findExisting(command);
        if (existing != null) {
            return toResult(existing);
        }

        BpmDefinitionNodeEntity routeNode = bpmDefinitionNodeDao.selectOne(
                Wrappers.<BpmDefinitionNodeEntity>lambdaQuery()
                        .eq(BpmDefinitionNodeEntity::getDefinitionId, instance.getDefinitionId())
                        .eq(BpmDefinitionNodeEntity::getNodeKey, command.routeNodeKey())
        );
        if (routeNode == null || routeNode.getCompiledNodeSnapshotJson() == null) {
            throw new IllegalArgumentException("ROUTE_SNAPSHOT_NOT_FOUND：路由节点冻结快照不存在");
        }

        JSONObject snapshot = JSON.parseObject(routeNode.getCompiledNodeSnapshotJson());
        RouteInput routeInput = routeInput(instance);
        Evaluation evaluation = evaluateBranches(snapshot, instance, routeInput.data());
        BpmRouteDecisionEntity entity = buildEntity(
                command, instance, routeNode, evaluation, routeInput.version()
        );
        try {
            bpmRouteDecisionDao.insert(entity);
        } catch (DuplicateKeyException ex) {
            BpmRouteDecisionEntity concurrent = findExisting(command);
            if (concurrent != null) {
                return toResult(concurrent);
            }
            throw ex;
        }
        return toResult(entity);
    }

    public void writeBranchVariables(
            DelegateExecution execution,
            String routeNodeKey,
            List<String> matchedBranchKeys
    ) {
        for (String branchKey : matchedBranchKeys) {
            execution.setVariable("route_" + routeNodeKey + "_" + branchKey, true);
        }
    }

    private Evaluation evaluateBranches(JSONObject snapshot, BpmInstanceEntity instance, JSONObject formData) {
        JSONArray branches = snapshot.getJSONArray("branches");
        if (branches == null || branches.isEmpty()) {
            throw new IllegalArgumentException("ROUTE_BRANCH_EMPTY：路由节点没有分支");
        }
        boolean exclusive = "EXCLUSIVE_BRANCH".equals(snapshot.getString("type"));
        List<String> matched = new ArrayList<>();
        JSONArray reasons = new JSONArray();
        String defaultBranchKey = null;

        for (Object rawBranch : branches) {
            if (!(rawBranch instanceof JSONObject branch)) {
                continue;
            }
            String branchKey = branch.getString("branchKey");
            if (branch.getBooleanValue("isDefault")) {
                defaultBranchKey = branchKey;
                continue;
            }
            RouteCondition condition = parseCondition(branch.getJSONObject("condition"));
            BpmRouteConditionResult conditionResult = conditionEvaluator.evaluate(
                    condition,
                    formData,
                    new BpmRouteExpressionContext(
                            instance.getInstanceId(),
                            instance.getFormDataVersion(),
                            new LinkedHashMap<>(formData),
                            buildInstanceContext(instance)
                    )
            );
            JSONObject reason = new JSONObject(true);
            reason.put("branchKey", branchKey);
            reason.put("matched", conditionResult.matched());
            reason.put("reasonCode", conditionResult.reasonCode());
            reason.put("reasonText", conditionResult.reasonText());
            reasons.add(reason);
            if (conditionResult.matched()) {
                matched.add(branchKey);
                if (exclusive) {
                    break;
                }
            }
        }

        boolean defaultUsed = matched.isEmpty();
        if (defaultUsed) {
            if (defaultBranchKey == null) {
                throw new IllegalArgumentException("ROUTE_DEFAULT_BRANCH_MISSING：路由节点缺少默认分支");
            }
            matched.add(defaultBranchKey);
        }
        return new Evaluation(List.copyOf(matched), defaultUsed, reasons.toJSONString());
    }

    private RouteCondition parseCondition(JSONObject condition) {
        if (condition == null) {
            return null;
        }
        JSONObject parameters = condition.getJSONObject("parameters");
        return new RouteCondition(
                condition.getString("sourceType"),
                condition.getString("fieldKey"),
                condition.getString("valueType"),
                condition.getString("operator"),
                condition.get("compareValue"),
                condition.getString("expressionKey"),
                condition.getInteger("version"),
                parameters == null ? Map.of() : new LinkedHashMap<>(parameters)
        );
    }

    private Map<String, Object> buildInstanceContext(BpmInstanceEntity instance) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (instance.getStartEmployeeId() != null) {
            context.put("startEmployeeId", instance.getStartEmployeeId());
        }
        if (instance.getStartDepartmentIdSnapshot() != null) {
            context.put("startDepartmentId", instance.getStartDepartmentIdSnapshot());
        }
        return context;
    }

    private RouteInput routeInput(BpmInstanceEntity instance) {
        if (instance.getRoutingFactSnapshotId() != null) {
            RoutingDataSnapshot routing = bpmApprovalRuntimeDataService.routingData(
                    instance.getRoutingFactSnapshotId()
            );
            JSONObject data = new JSONObject(true);
            data.putAll(routing.facts());
            return new RouteInput(data, routing.version());
        }
        JSONObject formData = JSON.parseObject(instance.getCurrentFormDataSnapshotJson());
        return new RouteInput(
                formData == null ? new JSONObject(true) : formData,
                instance.getFormDataVersion() == null ? 1L : instance.getFormDataVersion()
        );
    }

    private BpmRouteDecisionEntity buildEntity(
            BpmRouteDecisionCommand command,
            BpmInstanceEntity instance,
            BpmDefinitionNodeEntity routeNode,
            Evaluation evaluation,
            Long inputVersion
    ) {
        LocalDateTime now = LocalDateTime.now();
        BpmRouteDecisionEntity entity = new BpmRouteDecisionEntity();
        entity.setInstanceId(instance.getInstanceId());
        entity.setDefinitionId(instance.getDefinitionId());
        entity.setDefinitionNodeId(routeNode.getDefinitionNodeId());
        entity.setEngineProcessInstanceId(command.engineProcessInstanceId());
        entity.setRouteNodeKey(command.routeNodeKey());
        entity.setInputFormDataVersion(inputVersion);
        entity.setMatchedBranchKeysJson(JSON.toJSONString(evaluation.matchedBranchKeys()));
        entity.setDefaultBranchUsed(evaluation.defaultBranchUsed());
        entity.setEvaluationStatus("SUCCEEDED");
        entity.setReasonSnapshotJson(evaluation.reasonSnapshotJson());
        entity.setEvaluatedAt(now);
        return entity;
    }

    private BpmRouteDecisionEntity findExisting(BpmRouteDecisionCommand command) {
        return bpmRouteDecisionDao.selectByGenerationAndNode(
                command.instanceId(),
                command.engineProcessInstanceId(),
                command.routeNodeKey()
        );
    }

    private BpmRouteDecisionResult toResult(BpmRouteDecisionEntity entity) {
        List<String> matchedBranchKeys = JSON.parseArray(entity.getMatchedBranchKeysJson(), String.class);
        return new BpmRouteDecisionResult(
                entity.getRouteDecisionId(),
                matchedBranchKeys == null ? List.of() : matchedBranchKeys,
                Boolean.TRUE.equals(entity.getDefaultBranchUsed()),
                entity.getInputFormDataVersion()
        );
    }

    private record Evaluation(
            List<String> matchedBranchKeys,
            boolean defaultBranchUsed,
            String reasonSnapshotJson
    ) {
    }

    private record RouteInput(JSONObject data, Long version) {
    }
}
