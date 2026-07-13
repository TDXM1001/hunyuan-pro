package com.hunyuan.sa.bpm.module.runtime.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.exception.BusinessException;
import com.hunyuan.sa.bpm.module.definition.dao.BpmDefinitionNodeDao;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceCopyDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmInstanceDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmRouteDecisionDao;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceCopyEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmRouteDecisionEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmRuntimeGraphVO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 使用平台事实装配 authored 运行图，不从 Flowable 元素 ID 反向猜测。
 */
@Service
public class BpmRuntimeGraphService {

    @Resource private BpmInstanceDao bpmInstanceDao;
    @Resource private BpmDefinitionNodeDao bpmDefinitionNodeDao;
    @Resource private BpmTaskDao bpmTaskDao;
    @Resource private BpmInstanceCopyDao bpmInstanceCopyDao;
    @Resource private BpmRouteDecisionDao bpmRouteDecisionDao;
    @Resource private BpmGraphRuntimeMetadataService bpmGraphRuntimeMetadataService;

    public BpmRuntimeGraphVO build(Long instanceId, boolean employeeSafe) {
        BpmInstanceEntity instance = bpmInstanceDao.selectById(instanceId);
        if (instance == null) throw new BusinessException(UserErrorCode.DATA_NOT_EXIST);
        if ("GRAPH".equals(instance.getDefinitionSource())) {
            return buildGraph(instance, employeeSafe);
        }
        List<BpmDefinitionNodeEntity> definitionNodes = bpmDefinitionNodeDao.selectList(
                Wrappers.<BpmDefinitionNodeEntity>lambdaQuery()
                        .eq(BpmDefinitionNodeEntity::getDefinitionId, instance.getDefinitionId())
                        .orderByAsc(BpmDefinitionNodeEntity::getSortOrder)
        );
        List<BpmTaskEntity> tasks = bpmTaskDao.selectList(
                Wrappers.<BpmTaskEntity>lambdaQuery().eq(BpmTaskEntity::getInstanceId, instanceId)
        );
        List<BpmInstanceCopyEntity> copies = bpmInstanceCopyDao.selectList(
                Wrappers.<BpmInstanceCopyEntity>lambdaQuery().eq(BpmInstanceCopyEntity::getInstanceId, instanceId)
        );
        List<BpmRouteDecisionEntity> decisions = bpmRouteDecisionDao.queryByInstanceId(instanceId);

        Set<String> matchedBranchKeys = new HashSet<>();
        Set<String> decidedBranchKeys = collectDecidedBranchKeys(definitionNodes, decisions);
        decisions.forEach(item -> matchedBranchKeys.addAll(parseStringList(item.getMatchedBranchKeysJson())));
        Map<Long, List<BpmTaskEntity>> tasksByNode = new HashMap<>();
        tasks.forEach(task -> tasksByNode.computeIfAbsent(task.getDefinitionNodeId(), key -> new ArrayList<>()).add(task));
        Set<Long> copyNodeIds = new HashSet<>();
        copies.forEach(copy -> copyNodeIds.add(copy.getDefinitionNodeId()));
        Set<Long> routeNodeIds = new HashSet<>();
        decisions.forEach(decision -> routeNodeIds.add(decision.getDefinitionNodeId()));

        BpmRuntimeGraphVO graph = new BpmRuntimeGraphVO();
        graph.setInstanceId(instanceId);
        graph.setDefinitionId(instance.getDefinitionId());
        graph.setNodes(definitionNodes.stream()
                .sorted(Comparator.comparing(BpmDefinitionNodeEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(node -> toNode(node, tasksByNode.get(node.getDefinitionNodeId()), copyNodeIds,
                        routeNodeIds, decidedBranchKeys, matchedBranchKeys))
                .toList());
        graph.setRouteDecisions(decisions.stream().map(item -> toDecision(item, employeeSafe)).toList());
        return graph;
    }

    private BpmRuntimeGraphVO buildGraph(BpmInstanceEntity instance, boolean employeeSafe) {
        List<BpmTaskEntity> tasks = bpmTaskDao.selectList(
                Wrappers.<BpmTaskEntity>lambdaQuery().eq(BpmTaskEntity::getInstanceId, instance.getInstanceId())
        );
        List<BpmRouteDecisionEntity> decisions = bpmRouteDecisionDao.queryByInstanceId(instance.getInstanceId());
        Map<String, List<BpmTaskEntity>> tasksByAuthoredNode = new HashMap<>();
        tasks.stream()
                .filter(task -> StringUtils.isNotBlank(task.getTaskKey()))
                .forEach(task -> tasksByAuthoredNode
                        .computeIfAbsent(task.getTaskKey(), key -> new ArrayList<>())
                        .add(task));

        BpmRuntimeGraphVO graph = new BpmRuntimeGraphVO();
        graph.setInstanceId(instance.getInstanceId());
        graph.setGraphDefinitionVersionId(instance.getGraphDefinitionVersionId());
        graph.setNodes(bpmGraphRuntimeMetadataService.listNodes(instance.getGraphDefinitionVersionId()).stream()
                .map(node -> toGraphNode(node, tasksByAuthoredNode.get(node.authoredNodeId()), instance))
                .toList());
        graph.setRouteDecisions(decisions.stream().map(item -> toDecision(item, employeeSafe)).toList());
        return graph;
    }

    private BpmRuntimeGraphVO.Node toGraphNode(
            BpmGraphRuntimeMetadataService.GraphNodeMetadata metadata,
            List<BpmTaskEntity> tasks,
            BpmInstanceEntity instance
    ) {
        BpmRuntimeGraphVO.Node node = new BpmRuntimeGraphVO.Node();
        node.setNodeKey(metadata.authoredNodeId());
        node.setNodeName(metadata.nodeName());
        node.setNodeType(metadata.nodeType().name());
        node.setBranchPath(List.of());
        if (tasks != null && tasks.stream().anyMatch(task -> Integer.valueOf(1).equals(task.getTaskState()))) {
            node.setState("ACTIVE");
        } else if (tasks != null && tasks.stream().anyMatch(task -> Integer.valueOf(2).equals(task.getTaskState()))) {
            node.setState("COMPLETED");
        } else if (metadata.nodeType() == com.hunyuan.sa.bpm.engine.graph.GraphNodeType.START) {
            node.setState("COMPLETED");
        } else if (metadata.nodeType() == com.hunyuan.sa.bpm.engine.graph.GraphNodeType.END
                && Integer.valueOf(3).equals(instance.getRunState())) {
            node.setState("COMPLETED");
        } else {
            node.setState("NOT_ENTERED");
        }
        return node;
    }

    private BpmRuntimeGraphVO.Node toNode(
            BpmDefinitionNodeEntity entity,
            List<BpmTaskEntity> tasks,
            Set<Long> copyNodeIds,
            Set<Long> routeNodeIds,
            Set<String> decidedBranchKeys,
            Set<String> matchedBranchKeys
    ) {
        BpmRuntimeGraphVO.Node node = new BpmRuntimeGraphVO.Node();
        node.setDefinitionNodeId(entity.getDefinitionNodeId());
        node.setNodeKey(entity.getNodeKey());
        node.setNodeName(entity.getNodeNameSnapshot());
        node.setNodeType(entity.getNodeType());
        node.setSortOrder(entity.getSortOrder());
        List<String> branchPath = parseBranchPath(entity.getCompiledNodeSnapshotJson());
        node.setBranchPath(branchPath);
        node.setState(resolveState(entity.getDefinitionNodeId(), tasks, branchPath, copyNodeIds,
                routeNodeIds, decidedBranchKeys, matchedBranchKeys));
        return node;
    }

    private String resolveState(
            Long nodeId, List<BpmTaskEntity> tasks, List<String> branchPath,
            Set<Long> copyNodeIds, Set<Long> routeNodeIds,
            Set<String> decidedBranchKeys, Set<String> matchedBranchKeys
    ) {
        if (tasks != null && tasks.stream().anyMatch(task -> Integer.valueOf(1).equals(task.getTaskState()))) return "ACTIVE";
        if (tasks != null && tasks.stream().anyMatch(task -> Integer.valueOf(2).equals(task.getTaskState()))) return "COMPLETED";
        if (tasks != null && !tasks.isEmpty()) return "CANCELLED";
        if (routeNodeIds.contains(nodeId) || copyNodeIds.contains(nodeId)) return "COMPLETED";
        boolean pathWasDecided = branchPath.stream().anyMatch(decidedBranchKeys::contains);
        boolean pathMatched = branchPath.stream().anyMatch(matchedBranchKeys::contains);
        if (pathWasDecided && !pathMatched) return "SKIPPED";
        return "NOT_ENTERED";
    }

    private Set<String> collectDecidedBranchKeys(
            List<BpmDefinitionNodeEntity> nodes,
            List<BpmRouteDecisionEntity> decisions
    ) {
        Set<Long> decidedNodeIds = decisions.stream().map(BpmRouteDecisionEntity::getDefinitionNodeId).collect(java.util.stream.Collectors.toSet());
        Set<String> keys = new HashSet<>();
        nodes.stream().filter(node -> decidedNodeIds.contains(node.getDefinitionNodeId())).forEach(node -> {
            JSONObject snapshot = parseObject(node.getCompiledNodeSnapshotJson());
            JSONArray branches = snapshot.getJSONArray("branches");
            if (branches != null) branches.forEach(raw -> {
                if (raw instanceof JSONObject branch && StringUtils.isNotBlank(branch.getString("branchKey"))) {
                    keys.add(branch.getString("branchKey"));
                }
            });
        });
        return keys;
    }

    private BpmRuntimeGraphVO.RouteDecision toDecision(BpmRouteDecisionEntity entity, boolean employeeSafe) {
        BpmRuntimeGraphVO.RouteDecision vo = new BpmRuntimeGraphVO.RouteDecision();
        vo.setRouteDecisionId(entity.getRouteDecisionId());
        vo.setInstanceId(entity.getInstanceId());
        vo.setDefinitionId(entity.getDefinitionId());
        vo.setGraphDefinitionVersionId(entity.getGraphDefinitionVersionId());
        vo.setDefinitionNodeId(entity.getDefinitionNodeId());
        vo.setRouteNodeKey(entity.getRouteNodeKey());
        vo.setInputFormDataVersion(entity.getInputFormDataVersion());
        vo.setMatchedBranchKeys(parseStringList(entity.getMatchedBranchKeysJson()));
        vo.setDefaultBranchUsed(entity.getDefaultBranchUsed());
        vo.setEvaluationStatus(entity.getEvaluationStatus());
        vo.setReasonSnapshotJson(employeeSafe ? "{\"summary\":\"金额条件已满足\"}" : entity.getReasonSnapshotJson());
        vo.setEvaluatedAt(entity.getEvaluatedAt());
        return vo;
    }

    private List<String> parseBranchPath(String json) {
        return parseStringList(parseObject(json).getString("branchPath"));
    }

    private List<String> parseStringList(String json) {
        if (StringUtils.isBlank(json)) return List.of();
        try {
            JSONArray array = JSON.parseArray(json);
            return array == null ? List.of() : array.stream().map(String::valueOf).toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private JSONObject parseObject(String json) {
        if (StringUtils.isBlank(json)) return new JSONObject();
        try {
            JSONObject object = JSON.parseObject(json);
            return object == null ? new JSONObject() : object;
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }
}
