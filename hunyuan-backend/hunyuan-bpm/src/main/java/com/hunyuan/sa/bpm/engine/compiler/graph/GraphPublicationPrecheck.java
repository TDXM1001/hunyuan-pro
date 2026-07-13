package com.hunyuan.sa.bpm.engine.compiler.graph;

import com.hunyuan.sa.bpm.engine.graph.GraphCanonicalizer;
import com.hunyuan.sa.bpm.engine.graph.GraphEdge;
import com.hunyuan.sa.bpm.engine.graph.GraphNode;
import com.hunyuan.sa.bpm.engine.graph.GraphNodeType;
import com.hunyuan.sa.bpm.engine.graph.GraphValidationException;
import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * 发布前验证 Graph 的结构、端口和受控网关语义。
 */
public class GraphPublicationPrecheck {

    private static final String DEFAULT_PORT = "default";
    private static final String SPLIT_MODE = "SPLIT";
    private static final String JOIN_MODE = "JOIN";
    private static final Pattern PORT_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,63}");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_.]{0,127}");

    private final GraphCanonicalizer graphCanonicalizer = new GraphCanonicalizer();

    public GraphPublicationPrecheckResult check(HunyuanProcessDefinitionGraph graph) {
        List<GraphPublicationFinding> findings = new ArrayList<>();
        try {
            graphCanonicalizer.canonicalize(graph);
        } catch (GraphValidationException ex) {
            findings.add(finding("GRAPH_LOCAL_VALIDATION_FAILED", ex.getMessage(), null, "修正 Graph 的节点、边和作用域引用"));
            return new GraphPublicationPrecheckResult(false, List.copyOf(findings));
        }

        Map<String, GraphNode> nodes = new HashMap<>();
        Map<String, List<GraphEdge>> outgoing = new HashMap<>();
        Map<String, List<GraphEdge>> incoming = new HashMap<>();
        for (GraphNode node : graph.nodes()) {
            nodes.put(node.nodeId(), node);
            outgoing.put(node.nodeId(), new ArrayList<>());
            incoming.put(node.nodeId(), new ArrayList<>());
        }
        for (GraphEdge edge : graph.edges()) {
            outgoing.get(edge.sourceNodeId()).add(edge);
            incoming.get(edge.targetNodeId()).add(edge);
        }

        validateStartAndEnd(graph, incoming, outgoing, findings);
        validateTaskPortCounts(graph, incoming, outgoing, findings);
        validateGateways(graph, nodes, incoming, outgoing, findings);
        validateAdvancedNodes(graph, findings);
        validateSlaPolicies(graph, findings);
        validateReachability(graph, outgoing, incoming, findings);
        if (hasCycle(nodes.keySet(), outgoing)) {
            findings.add(finding("GRAPH_CYCLE_FORBIDDEN", "M1 发布图不允许回边或环", null, "移除形成环的连线"));
        }
        return new GraphPublicationPrecheckResult(findings.isEmpty(), List.copyOf(findings));
    }

    private void validateSlaPolicies(HunyuanProcessDefinitionGraph graph, List<GraphPublicationFinding> findings) {
        for (GraphNode node : graph.nodes()) {
            Object raw = node.properties().get("taskSlaPolicy");
            if (raw == null) continue;
            if (!(raw instanceof Map<?, ?> policy)) {
                findings.add(finding("SLA_POLICY_INVALID", "任务 SLA 必须是结构化策略", node.nodeId(), "重新配置 SLA"));
                continue;
            }
            String dueAfter = String.valueOf(policy.get("dueAfter"));
            try {
                Duration duration = Duration.parse(dueAfter);
                if (duration.isNegative() || duration.isZero()) throw new DateTimeParseException("invalid", dueAfter, 0);
            } catch (DateTimeParseException ex) {
                findings.add(finding("SLA_DURATION_INVALID", "SLA 到期时长必须是正 ISO-8601 duration", node.nodeId(), "例如 PT2H"));
            }
            String action = String.valueOf(policy.get("timeoutAction"));
            if (!Set.of("NONE", "REMIND_ONLY", "AUTO_APPROVE", "AUTO_REJECT", "ASSIGN_ADMIN").contains(action)) {
                findings.add(finding("SLA_ACTION_INVALID", "SLA 超时动作不受支持", node.nodeId(), "选择登记动作"));
            }
            if (Set.of("AUTO_APPROVE", "AUTO_REJECT").contains(action)
                    && !"LOW".equals(stringProperty(graph.policies(), "riskLevel"))) {
                findings.add(finding("SLA_AUTO_TERMINAL_RISK_FORBIDDEN", "只有低风险流程可配置 SLA 自动终态", node.nodeId(), "改为提醒或人工处置"));
            }
        }
    }

    private void validateAdvancedNodes(
            HunyuanProcessDefinitionGraph graph,
            List<GraphPublicationFinding> findings
    ) {
        for (GraphNode node : graph.nodes()) {
            switch (node.type()) {
                case DELAY -> validateDelay(node, findings);
                case EXTERNAL_TRIGGER -> validateExternalTrigger(node, findings);
                case SUB_PROCESS -> validateSubProcess(node, findings);
                default -> {
                }
            }
        }
    }

    private void validateDelay(GraphNode node, List<GraphPublicationFinding> findings) {
        String mode = stringProperty(node.properties(), "mode");
        String value = stringProperty(node.properties(), "value");
        boolean valid = false;
        try {
            if ("DURATION".equals(mode)) {
                valid = !Duration.parse(value).isNegative() && !Duration.parse(value).isZero();
            } else if ("FIXED_DATETIME".equals(mode)) {
                OffsetDateTime.parse(value);
                valid = true;
            } else if ("FORM_DATETIME".equals(mode)) {
                valid = StringUtils.isNotBlank(value) && StringUtils.isNotBlank(stringProperty(node.properties(), "timezone"));
            }
        } catch (DateTimeParseException | NullPointerException ignored) {
            valid = false;
        }
        if (!valid) {
            findings.add(finding("DELAY_VALUE_INVALID", "延迟节点必须使用受控时长、带偏移时间或表单日期字段", node.nodeId(), "修正延迟模式和值"));
        }
    }

    private void validateExternalTrigger(GraphNode node, List<GraphPublicationFinding> findings) {
        Map<String, Object> properties = node.properties();
        if (StringUtils.isBlank(stringProperty(properties, "connectorKey"))
                || StringUtils.isBlank(stringProperty(properties, "operationKey"))) {
            findings.add(finding("EXTERNAL_CONNECTOR_REFERENCE_INVALID", "外部节点必须引用登记连接器和操作", node.nodeId(), "选择登记连接器与操作"));
        }
        if (positiveInteger(properties.get("connectorVersion")) == null) {
            findings.add(finding("EXTERNAL_CONNECTOR_VERSION_REQUIRED", "外部节点必须冻结连接器版本", node.nodeId(), "选择已发布连接器版本"));
        }
        if (properties.containsKey("url") || properties.containsKey("endpoint")) {
            findings.add(finding("EXTERNAL_ENDPOINT_FORBIDDEN", "流程定义不得保存外部端点", node.nodeId(), "仅保存登记连接器引用"));
        }
        if (properties.containsKey("credential") || properties.containsKey("secret") || properties.containsKey("token")) {
            findings.add(finding("EXTERNAL_CREDENTIAL_FORBIDDEN", "流程定义不得保存凭据", node.nodeId(), "由连接器环境引用解析凭据"));
        }
        String waitMode = stringProperty(properties, "waitMode");
        if (!Set.of("NO_WAIT", "WAIT_CALLBACK").contains(waitMode)) {
            findings.add(finding("EXTERNAL_WAIT_MODE_INVALID", "外部节点等待模式不受支持", node.nodeId(), "选择 NO_WAIT 或 WAIT_CALLBACK"));
        }
        if ("WAIT_CALLBACK".equals(waitMode)) {
            Object rawPolicy = properties.get("timeoutPolicy");
            String timeoutAfter = rawPolicy instanceof Map<?, ?> policy
                    ? String.valueOf(policy.get("timeoutAfter")) : null;
            try {
                if (StringUtils.isBlank(timeoutAfter) || Duration.parse(timeoutAfter).isNegative()
                        || Duration.parse(timeoutAfter).isZero()) {
                    throw new DateTimeParseException("invalid", String.valueOf(timeoutAfter), 0);
                }
            } catch (DateTimeParseException ex) {
                findings.add(finding("EXTERNAL_TIMEOUT_POLICY_REQUIRED", "回调等待必须配置正时长超时策略", node.nodeId(), "配置 timeoutPolicy.timeoutAfter"));
            }
        }
    }

    private void validateSubProcess(GraphNode node, List<GraphPublicationFinding> findings) {
        Map<String, Object> properties = node.properties();
        if (StringUtils.isBlank(stringProperty(properties, "calledProcessKey"))) {
            findings.add(finding("SUB_PROCESS_REFERENCE_REQUIRED", "子流程节点必须引用流程 key", node.nodeId(), "选择已发布子流程"));
        }
        if (positiveInteger(properties.get("calledDefinitionVersionId")) == null) {
            findings.add(finding("SUB_PROCESS_VERSION_REQUIRED", "子流程节点必须冻结定义版本", node.nodeId(), "选择已发布子流程版本"));
        }
        if (StringUtils.isBlank(stringProperty(properties, "calledEngineProcessDefinitionId"))) {
            findings.add(finding("SUB_PROCESS_ENGINE_DEFINITION_REQUIRED", "子流程节点必须冻结引擎定义ID", node.nodeId(), "重新发布以解析子流程定义"));
        }
        String failurePolicy = stringProperty(properties, "failurePolicy");
        if (failurePolicy == null || !Set.of("PAUSE_PARENT", "REJECT_PARENT", "MANUAL_INTERVENTION")
                .contains(failurePolicy)) {
            findings.add(finding("SUB_PROCESS_FAILURE_POLICY_REQUIRED", "子流程必须配置失败传播策略", node.nodeId(), "选择暂停、拒绝或人工处置"));
        }
        String cancelPropagation = stringProperty(properties, "cancelPropagation");
        if (cancelPropagation == null || !Set.of("CANCEL_CHILD", "KEEP_CHILD")
                .contains(cancelPropagation)) {
            findings.add(finding("SUB_PROCESS_CANCEL_POLICY_REQUIRED", "子流程必须配置父取消传播策略", node.nodeId(), "选择取消子流程或保留子流程"));
        }
        validateVariableMapping(node, "inputMapping", findings);
        validateVariableMapping(node, "outputMapping", findings);
    }

    private void validateVariableMapping(GraphNode node, String property, List<GraphPublicationFinding> findings) {
        Object value = node.properties().get(property);
        if (value == null) return;
        if (!(value instanceof Map<?, ?> mapping)) {
            findings.add(finding("SUB_PROCESS_MAPPING_INVALID", "子流程变量映射必须是键值对象", node.nodeId(), "配置结构化变量映射"));
            return;
        }
        for (Map.Entry<?, ?> entry : mapping.entrySet()) {
            String target = String.valueOf(entry.getKey());
            String source = String.valueOf(entry.getValue());
            if (!VARIABLE_PATTERN.matcher(target).matches() || !VARIABLE_PATTERN.matcher(source).matches()) {
                findings.add(finding("SUB_PROCESS_MAPPING_INVALID", "子流程变量映射只允许稳定变量标识", node.nodeId(), "移除表达式并使用变量名"));
                return;
            }
        }
    }

    private void validateStartAndEnd(
            HunyuanProcessDefinitionGraph graph,
            Map<String, List<GraphEdge>> incoming,
            Map<String, List<GraphEdge>> outgoing,
            List<GraphPublicationFinding> findings
    ) {
        List<GraphNode> starts = graph.nodes().stream().filter(node -> node.type() == GraphNodeType.START).toList();
        List<GraphNode> ends = graph.nodes().stream().filter(node -> node.type() == GraphNodeType.END).toList();
        if (starts.size() != 1) {
            findings.add(finding("START_NODE_COUNT_INVALID", "发布图必须且只能有一个开始节点", null, "保留一个开始节点"));
        }
        if (ends.size() != 1) {
            findings.add(finding("END_NODE_COUNT_INVALID", "发布图必须且只能有一个结束节点", null, "保留一个结束节点"));
        }
        if (starts.size() == 1) {
            String nodeId = starts.get(0).nodeId();
            if (!incoming.get(nodeId).isEmpty()) {
                findings.add(finding("START_NODE_HAS_INCOMING", "开始节点不能有入边", nodeId, "移除指向开始节点的连线"));
            }
            if (outgoing.get(nodeId).size() != 1) {
                findings.add(finding("START_NODE_OUTGOING_INVALID", "开始节点必须且只能有一条出边", nodeId, "连接一个首个业务节点"));
            }
        }
        if (ends.size() == 1) {
            String nodeId = ends.get(0).nodeId();
            if (!outgoing.get(nodeId).isEmpty()) {
                findings.add(finding("END_NODE_HAS_OUTGOING", "结束节点不能有出边", nodeId, "移除结束节点的出边"));
            }
            if (incoming.get(nodeId).size() != 1) {
                findings.add(finding("END_NODE_INCOMING_INVALID", "结束节点必须且只能有一条入边", nodeId, "通过一个末尾节点连接结束节点"));
            }
        }
    }

    private void validateTaskPortCounts(
            HunyuanProcessDefinitionGraph graph,
            Map<String, List<GraphEdge>> incoming,
            Map<String, List<GraphEdge>> outgoing,
            List<GraphPublicationFinding> findings
    ) {
        for (GraphNode node : graph.nodes()) {
            if (node.type() == GraphNodeType.START || node.type() == GraphNodeType.END || isGateway(node.type())) {
                continue;
            }
            if (incoming.get(node.nodeId()).size() != 1 || outgoing.get(node.nodeId()).size() != 1) {
                findings.add(finding(
                        "NODE_PORT_COUNT_INVALID",
                        "非网关节点必须且只能有一条入边和一条出边",
                        node.nodeId(),
                        "通过网关表达分叉或汇合，不要让任务节点承担控制流"
                ));
            }
        }
    }

    private void validateGateways(
            HunyuanProcessDefinitionGraph graph,
            Map<String, GraphNode> nodes,
            Map<String, List<GraphEdge>> incoming,
            Map<String, List<GraphEdge>> outgoing,
            List<GraphPublicationFinding> findings
    ) {
        for (GraphNode node : graph.nodes()) {
            if (!isGateway(node.type())) {
                continue;
            }
            String mode = stringProperty(node.properties(), "gatewayMode");
            String pairedGatewayId = stringProperty(node.properties(), "pairedGatewayId");
            if (!SPLIT_MODE.equals(mode) && !JOIN_MODE.equals(mode)) {
                findings.add(finding("GATEWAY_MODE_INVALID", "网关必须声明 SPLIT 或 JOIN 模式", node.nodeId(), "设置网关模式"));
                continue;
            }
            GraphNode pairedGateway = nodes.get(pairedGatewayId);
            validateGatewayPair(node, mode, pairedGateway, findings);
            if (SPLIT_MODE.equals(mode)) {
                validateSplit(node, incoming.get(node.nodeId()), outgoing.get(node.nodeId()), findings);
                if (pairedGateway != null && JOIN_MODE.equals(stringProperty(pairedGateway.properties(), "gatewayMode"))) {
                    validateSplitBranchesReachJoin(node, pairedGateway, outgoing, findings);
                }
            } else {
                validateJoin(node, incoming.get(node.nodeId()), outgoing.get(node.nodeId()), findings);
            }
        }
    }

    private void validateGatewayPair(
            GraphNode node,
            String mode,
            GraphNode pairedGateway,
            List<GraphPublicationFinding> findings
    ) {
        if (pairedGateway == null) {
            findings.add(finding("GATEWAY_PAIR_NOT_FOUND", "网关缺少配对的汇合或分叉节点", node.nodeId(), "选择同作用域的配对网关"));
            return;
        }
        if (pairedGateway.type() != node.type() || !pairedGateway.scopeId().equals(node.scopeId())) {
            findings.add(finding("GATEWAY_PAIR_TYPE_INVALID", "配对网关必须同类型且位于同一作用域", node.nodeId(), "选择同类型的配对网关"));
        }
        String pairedMode = stringProperty(pairedGateway.properties(), "gatewayMode");
        if (mode.equals(pairedMode) || (!SPLIT_MODE.equals(pairedMode) && !JOIN_MODE.equals(pairedMode))) {
            findings.add(finding("GATEWAY_PAIR_MODE_INVALID", "配对网关必须一端分叉、一端汇合", node.nodeId(), "将配对网关设置为相反模式"));
        }
        if (!node.nodeId().equals(stringProperty(pairedGateway.properties(), "pairedGatewayId"))) {
            findings.add(finding("GATEWAY_PAIR_REFERENCE_INVALID", "配对网关必须双向引用", node.nodeId(), "让两个网关互相引用"));
        }
    }

    private void validateSplit(
            GraphNode node,
            List<GraphEdge> incoming,
            List<GraphEdge> outgoing,
            List<GraphPublicationFinding> findings
    ) {
        if (incoming.size() != 1) {
            findings.add(finding("GATEWAY_SPLIT_INCOMING_INVALID", "分叉网关必须且只能有一条入边", node.nodeId(), "将分叉前的流程收敛到一条入边"));
        }
        if (outgoing.size() < 2) {
            findings.add(finding("GATEWAY_SPLIT_OUTGOING_INSUFFICIENT", "分叉网关至少需要两条出边", node.nodeId(), "增加至少两条分支连线"));
            return;
        }

        Set<String> ports = new HashSet<>();
        int defaultCount = 0;
        for (GraphEdge edge : outgoing) {
            String port = edge.sourcePort();
            if (StringUtils.isBlank(port) || !PORT_PATTERN.matcher(port).matches()) {
                findings.add(finding("GATEWAY_BRANCH_PORT_INVALID", "分支端口必须是稳定标识", edge.edgeId(), "使用字母或下划线开头的分支端口"));
                continue;
            }
            if (!ports.add(port)) {
                findings.add(finding("GATEWAY_BRANCH_PORT_DUPLICATE", "同一分叉网关的分支端口不能重复", edge.edgeId(), "为每条分支配置唯一端口"));
            }
            if (DEFAULT_PORT.equals(port)) {
                defaultCount++;
            }
            if (isConditionalGateway(node.type()) && !DEFAULT_PORT.equals(port)) {
                validateRouteCondition(edge, findings);
            }
        }

        if (isConditionalGateway(node.type())) {
            if (defaultCount == 0) {
                findings.add(finding("GATEWAY_DEFAULT_ROUTE_MISSING", "条件或包容网关必须有一条默认分支", node.nodeId(), "为一个分支端口设置 default"));
            } else if (defaultCount > 1) {
                findings.add(finding("GATEWAY_DEFAULT_ROUTE_DUPLICATE", "条件或包容网关只能有一条默认分支", node.nodeId(), "保留唯一的 default 分支"));
            }
        } else if (defaultCount > 0) {
            findings.add(finding("GATEWAY_PARALLEL_DEFAULT_FORBIDDEN", "并行网关不允许默认分支", node.nodeId(), "为并行分支使用业务端口"));
        }
    }

    private void validateJoin(
            GraphNode node,
            List<GraphEdge> incoming,
            List<GraphEdge> outgoing,
            List<GraphPublicationFinding> findings
    ) {
        if (incoming.size() < 2) {
            findings.add(finding("GATEWAY_JOIN_INCOMING_INSUFFICIENT", "汇合网关至少需要两条入边", node.nodeId(), "连接所有分支到该汇合网关"));
        }
        if (outgoing.size() != 1) {
            findings.add(finding("GATEWAY_JOIN_OUTGOING_INVALID", "汇合网关必须且只能有一条出边", node.nodeId(), "将汇合后的流程连接为单一路径"));
        }
    }

    private void validateRouteCondition(GraphEdge edge, List<GraphPublicationFinding> findings) {
        Object rawCondition = edge.properties().get("routeCondition");
        if (!(rawCondition instanceof Map<?, ?> rawMap)) {
            findings.add(finding("GATEWAY_ROUTE_CONDITION_MISSING", "非默认条件分支必须配置类型化路由条件", edge.edgeId(), "配置表单字段、上下文或登记表达式条件"));
            return;
        }
        Map<String, Object> condition = new HashMap<>();
        rawMap.forEach((key, value) -> condition.put(String.valueOf(key), value));
        String sourceType = stringProperty(condition, "sourceType");
        if ("REGISTERED_EXPRESSION".equals(sourceType)) {
            if (StringUtils.isBlank(stringProperty(condition, "expressionKey")) || positiveInteger(condition.get("expressionVersion")) == null) {
                findings.add(finding("GATEWAY_ROUTE_EXPRESSION_INVALID", "登记表达式必须提供 key 和正整数版本", edge.edgeId(), "选择已登记的表达式版本"));
            }
            return;
        }
        if (!"FORM_FIELD".equals(sourceType) && !"INSTANCE_CONTEXT".equals(sourceType)) {
            findings.add(finding("GATEWAY_ROUTE_SOURCE_INVALID", "路由条件来源必须是表单字段、实例上下文或登记表达式", edge.edgeId(), "选择受控条件来源"));
            return;
        }
        if (StringUtils.isBlank(stringProperty(condition, "fieldKey"))
                || StringUtils.isBlank(stringProperty(condition, "valueType"))
                || StringUtils.isBlank(stringProperty(condition, "operator"))) {
            findings.add(finding("GATEWAY_ROUTE_CONDITION_INVALID", "路由条件必须包含字段、值类型和操作符", edge.edgeId(), "补全类型化路由条件"));
        }
    }

    private void validateSplitBranchesReachJoin(
            GraphNode split,
            GraphNode join,
            Map<String, List<GraphEdge>> outgoing,
            List<GraphPublicationFinding> findings
    ) {
        for (GraphEdge edge : outgoing.get(split.nodeId())) {
            if (!canReach(edge.targetNodeId(), join.nodeId(), outgoing)) {
                findings.add(finding("GATEWAY_BRANCH_CANNOT_REACH_JOIN", "分支无法到达配对的汇合网关", edge.edgeId(), "将分支连接回配对汇合网关"));
            }
        }
    }

    private void validateReachability(
            HunyuanProcessDefinitionGraph graph,
            Map<String, List<GraphEdge>> outgoing,
            Map<String, List<GraphEdge>> incoming,
            List<GraphPublicationFinding> findings
    ) {
        GraphNode start = graph.nodes().stream().filter(node -> node.type() == GraphNodeType.START).findFirst().orElse(null);
        GraphNode end = graph.nodes().stream().filter(node -> node.type() == GraphNodeType.END).findFirst().orElse(null);
        if (start != null) {
            Set<String> reachable = traverse(start.nodeId(), outgoing, true);
            for (GraphNode node : graph.nodes()) {
                if (!reachable.contains(node.nodeId())) {
                    findings.add(finding("NODE_UNREACHABLE", "节点不可从开始节点到达", node.nodeId(), "连接节点或删除孤立节点"));
                }
            }
        }
        if (end != null) {
            Set<String> reachingEnd = traverse(end.nodeId(), incoming, false);
            for (GraphNode node : graph.nodes()) {
                if (!reachingEnd.contains(node.nodeId())) {
                    findings.add(finding("NODE_CANNOT_REACH_END", "节点无法到达结束节点", node.nodeId(), "补充通往结束节点的路径"));
                }
            }
        }
    }

    private Set<String> traverse(String startNodeId, Map<String, List<GraphEdge>> adjacency, boolean forward) {
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> pending = new ArrayDeque<>();
        pending.add(startNodeId);
        while (!pending.isEmpty()) {
            String nodeId = pending.removeFirst();
            if (!visited.add(nodeId)) {
                continue;
            }
            for (GraphEdge edge : adjacency.getOrDefault(nodeId, List.of())) {
                pending.add(forward ? edge.targetNodeId() : edge.sourceNodeId());
            }
        }
        return visited;
    }

    private boolean canReach(String startNodeId, String targetNodeId, Map<String, List<GraphEdge>> outgoing) {
        return traverse(startNodeId, outgoing, true).contains(targetNodeId);
    }

    private boolean hasCycle(Set<String> nodeIds, Map<String, List<GraphEdge>> outgoing) {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (String nodeId : nodeIds) {
            if (hasCycle(nodeId, outgoing, visiting, visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCycle(
            String nodeId,
            Map<String, List<GraphEdge>> outgoing,
            Set<String> visiting,
            Set<String> visited
    ) {
        if (visited.contains(nodeId)) {
            return false;
        }
        if (!visiting.add(nodeId)) {
            return true;
        }
        for (GraphEdge edge : outgoing.getOrDefault(nodeId, List.of())) {
            if (hasCycle(edge.targetNodeId(), outgoing, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(nodeId);
        visited.add(nodeId);
        return false;
    }

    private boolean isGateway(GraphNodeType type) {
        return type == GraphNodeType.CONDITION
                || type == GraphNodeType.PARALLEL_GATEWAY
                || type == GraphNodeType.INCLUSIVE_GATEWAY;
    }

    private boolean isConditionalGateway(GraphNodeType type) {
        return type == GraphNodeType.CONDITION || type == GraphNodeType.INCLUSIVE_GATEWAY;
    }

    private String stringProperty(Map<String, Object> properties, String key) {
        Object value = properties.get(key);
        return value instanceof String text ? text : null;
    }

    private Integer positiveInteger(Object value) {
        return value instanceof Number number && number.intValue() > 0 && number.doubleValue() == number.intValue()
                ? number.intValue()
                : null;
    }

    private GraphPublicationFinding finding(String code, String message, String authoredElementId, String repairHint) {
        return new GraphPublicationFinding(code, message, authoredElementId, repairHint);
    }
}
