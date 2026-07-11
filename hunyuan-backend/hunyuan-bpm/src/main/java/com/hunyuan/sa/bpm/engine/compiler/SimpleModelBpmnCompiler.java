package com.hunyuan.sa.bpm.engine.compiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.engine.ast.BranchNode;
import com.hunyuan.sa.bpm.engine.ast.CopyTaskNode;
import com.hunyuan.sa.bpm.engine.ast.HumanTaskNode;
import com.hunyuan.sa.bpm.engine.ast.ProcessAst;
import com.hunyuan.sa.bpm.engine.ast.ProcessBranch;
import com.hunyuan.sa.bpm.engine.ast.ProcessNode;
import com.hunyuan.sa.bpm.engine.ast.ProcessNodeType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SimpleModel -> BPMN XML 的受限编译器。
 */
@Component
public class SimpleModelBpmnCompiler {

    /**
     * 按 authored 节点顺序编译；并行能力只存在于单个 parallelAll 固定片段内部。
     */
    public CompiledDefinitionArtifact compile(
            String modelKey,
            String modelName,
            String simpleModelJson,
            String startRuleJson,
            String variableMappingJson
    ) {
        JSONObject simpleModelObject = JSON.parseObject(simpleModelJson);
        if (simpleModelObject.getInteger("schemaVersion") != null) {
            return compileV2(modelKey, modelName, simpleModelJson, startRuleJson, variableMappingJson);
        }
        JSONArray nodes = simpleModelObject.getJSONArray("nodes");
        List<CompiledNodeSnapshot> nodeSnapshots = new ArrayList<>();
        List<LegacyBpmnFragment> fragments = new ArrayList<>();

        if (nodes != null) {
            for (int index = 0; index < nodes.size(); index++) {
                JSONObject nodeObject = nodes.getJSONObject(index);
                if (nodeObject == null) {
                    continue;
                }
                compileNode(
                        nodeObject,
                        index,
                        startRuleJson,
                        variableMappingJson,
                        nodeSnapshots,
                        fragments
                );
            }
        }

        return new CompiledDefinitionArtifact(
                buildBpmnXml(modelKey, modelName, fragments),
                nodeSnapshots
        );
    }

    private void compileNode(
            JSONObject nodeObject,
            int index,
            String startRuleJson,
            String variableMappingJson,
            List<CompiledNodeSnapshot> nodeSnapshots,
            List<LegacyBpmnFragment> fragments
    ) {
        String nodeKey = firstNonBlank(
                nodeObject.getString("nodeKey"),
                nodeObject.getString("id"),
                "task_" + (index + 1)
        );
        String nodeType = firstNonBlank(nodeObject.getString("type"), "userTask");
        String nodeName = firstNonBlank(nodeObject.getString("name"), "审批节点" + (index + 1));

        if (isMultipleEmployeeApproval(nodeObject, nodeType)) {
            JSONArray employeeIds = nodeObject.getJSONArray("employeeIds");
            List<UserTaskNode> expandedTasks = new ArrayList<>();
            for (int memberIndex = 0; memberIndex < employeeIds.size(); memberIndex++) {
                String expandedNodeKey = nodeKey + "_" + (memberIndex + 1);
                String expandedNodeName = nodeName + "（" + (memberIndex + 1) + "/" + employeeIds.size() + "）";
                JSONObject compiledNodeObject = buildCompiledNodeObject(
                        nodeObject,
                        expandedNodeKey,
                        nodeType,
                        expandedNodeName,
                        startRuleJson,
                        variableMappingJson
                );
                compiledNodeObject.put("employeeId", employeeIds.getLongValue(memberIndex));
                compiledNodeObject.put("authoredNodeKey", nodeKey);
                compiledNodeObject.put("authoredNodeName", nodeName);
                // 多人审批的运行时投影使用 authored 节点作为稳定审批组身份。
                compiledNodeObject.put("approvalGroupKey", nodeKey);
                compiledNodeObject.put("approvalGroupName", nodeName);
                if (isParallelAll(nodeObject)) {
                    compiledNodeObject.put("parallelIndex", memberIndex + 1);
                    compiledNodeObject.put("parallelTotal", employeeIds.size());
                } else {
                    compiledNodeObject.put("sequentialIndex", memberIndex + 1);
                    compiledNodeObject.put("sequentialTotal", employeeIds.size());
                }
                nodeSnapshots.add(buildSnapshot(
                        nodeObject,
                        expandedNodeKey,
                        nodeType,
                        expandedNodeName,
                        compiledNodeObject,
                        nodeSnapshots.size() + 1
                ));
                expandedTasks.add(new UserTaskNode(expandedNodeKey, expandedNodeName));
            }
            if (isParallelAll(nodeObject)) {
                fragments.add(new ParallelAllFragment(
                        "gateway_" + nodeKey + "_split",
                        expandedTasks,
                        "gateway_" + nodeKey + "_join"
                ));
            } else {
                expandedTasks.forEach(task -> fragments.add(new UserTaskFragment(task)));
            }
            return;
        }

        JSONObject compiledNodeObject = buildCompiledNodeObject(
                nodeObject,
                nodeKey,
                nodeType,
                nodeName,
                startRuleJson,
                variableMappingJson
        );
        nodeSnapshots.add(buildSnapshot(
                nodeObject,
                nodeKey,
                nodeType,
                nodeName,
                compiledNodeObject,
                nodeSnapshots.size() + 1
        ));
        if ("userTask".equals(nodeType)) {
            fragments.add(new UserTaskFragment(new UserTaskNode(nodeKey, nodeName)));
        }
    }

    private CompiledNodeSnapshot buildSnapshot(
            JSONObject authoredNode,
            String nodeKey,
            String nodeType,
            String nodeName,
            JSONObject compiledNode,
            int sortOrder
    ) {
        return new CompiledNodeSnapshot(
                nodeKey,
                nodeType,
                nodeName,
                sortOrder,
                JSON.toJSONString(authoredNode),
                JSON.toJSONString(compiledNode)
        );
    }

    private JSONObject buildCompiledNodeObject(
            JSONObject nodeObject,
            String nodeKey,
            String nodeType,
            String nodeName,
            String startRuleJson,
            String variableMappingJson
    ) {
        JSONObject compiledNodeObject = new JSONObject();
        compiledNodeObject.put("nodeKey", nodeKey);
        compiledNodeObject.put("name", nodeName);
        compiledNodeObject.put("type", nodeType);
        compiledNodeObject.put("approvalMode", nodeObject.getString("approvalMode"));
        compiledNodeObject.put("fieldPermissions", nodeObject.get("fieldPermissions"));
        compiledNodeObject.put("candidateResolverType", firstNonBlank(
                nodeObject.getString("candidateResolverType"),
                nodeObject.getString("resolverType")
        ));
        compiledNodeObject.put("listeners", nodeObject.get("listeners"));
        compiledNodeObject.put("variableMappingJson", variableMappingJson);
        compiledNodeObject.put("startRuleJson", startRuleJson);
        return compiledNodeObject;
    }

    private boolean isMultipleEmployeeApproval(JSONObject nodeObject, String nodeType) {
        String approvalMode = nodeObject.getString("approvalMode");
        return "userTask".equals(nodeType)
                && ("sequential".equalsIgnoreCase(approvalMode) || "parallelAll".equalsIgnoreCase(approvalMode))
                && "EMPLOYEE".equalsIgnoreCase(firstNonBlank(
                nodeObject.getString("candidateResolverType"),
                nodeObject.getString("resolverType")
        ))
                && nodeObject.getJSONArray("employeeIds") != null
                && !nodeObject.getJSONArray("employeeIds").isEmpty();
    }

    private boolean isParallelAll(JSONObject nodeObject) {
        return "parallelAll".equalsIgnoreCase(nodeObject.getString("approvalMode"));
    }

    private CompiledDefinitionArtifact compileV2(
            String modelKey,
            String modelName,
            String simpleModelJson,
            String startRuleJson,
            String variableMappingJson
    ) {
        ProcessAst ast = new ProcessAstParser().parse(simpleModelJson);
        V2Compilation compilation = new V2Compilation(startRuleJson, variableMappingJson);
        BpmnFragment body = compilation.compileNodes(ast.nodes(), List.of());
        return new CompiledDefinitionArtifact(
                buildV2BpmnXml(modelKey, modelName, body, compilation.idFactory),
                body.compiledNodeSnapshots()
        );
    }

    private String buildV2BpmnXml(
            String modelKey,
            String modelName,
            BpmnFragment body,
            StableBpmnIdFactory idFactory
    ) {
        List<BpmnSequenceFlow> flows = new ArrayList<>(body.sequenceFlows());
        if (!body.entryElementIds().isEmpty()) {
            for (String entry : body.entryElementIds()) {
                flows.add(new BpmnSequenceFlow(idFactory.nextFlowId("startEvent", entry), "startEvent", entry, null));
            }
            for (String exit : body.exitElementIds()) {
                flows.add(new BpmnSequenceFlow(idFactory.nextFlowId(exit, "endEvent"), exit, "endEvent", null));
            }
        } else {
            flows.add(new BpmnSequenceFlow(idFactory.nextFlowId("startEvent", "endEvent"), "startEvent", "endEvent", null));
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" ");
        xml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        xml.append("xmlns:flowable=\"http://flowable.org/bpmn\" targetNamespace=\"http://hunyuan.sa/bpm\">");
        xml.append("<process id=\"").append(escapeXml(modelKey)).append("\" name=\"")
                .append(escapeXml(modelName)).append("\" isExecutable=\"true\">");
        xml.append("<startEvent id=\"startEvent\" name=\"开始\"/>");
        body.generatedElements().forEach(xml::append);
        xml.append("<endEvent id=\"endEvent\" name=\"结束\"/>");
        flows.forEach(flow -> appendV2SequenceFlow(xml, flow));
        xml.append("</process></definitions>");
        return xml.toString();
    }

    private void appendV2SequenceFlow(StringBuilder xml, BpmnSequenceFlow flow) {
        xml.append("<sequenceFlow id=\"").append(escapeXml(flow.flowId()))
                .append("\" sourceRef=\"").append(escapeXml(flow.sourceRef()))
                .append("\" targetRef=\"").append(escapeXml(flow.targetRef())).append("\"");
        if (StringUtils.isBlank(flow.conditionExpression())) {
            xml.append("/>");
            return;
        }
        xml.append("><conditionExpression xsi:type=\"tFormalExpression\"><![CDATA[")
                .append(flow.conditionExpression())
                .append("]]></conditionExpression></sequenceFlow>");
    }

    /**
     * v2 编译状态只在一次 compile 调用内存在，避免并发发布共享计数器。
     */
    private final class V2Compilation {

        private final String startRuleJson;
        private final String variableMappingJson;
        private final StableBpmnIdFactory idFactory = new StableBpmnIdFactory();
        private final FragmentComposer composer = new FragmentComposer();
        private int snapshotOrder;

        private V2Compilation(String startRuleJson, String variableMappingJson) {
            this.startRuleJson = startRuleJson;
            this.variableMappingJson = variableMappingJson;
        }

        private BpmnFragment compileNodes(List<ProcessNode> nodes, List<String> branchPath) {
            List<BpmnFragment> fragments = nodes.stream()
                    .map(node -> compileNode(node, branchPath))
                    .toList();
            return composer.compose(fragments, idFactory);
        }

        private BpmnFragment compileNode(ProcessNode node, List<String> branchPath) {
            if (node instanceof HumanTaskNode humanTaskNode) {
                return compileHumanTask(humanTaskNode, branchPath);
            }
            if (node instanceof BranchNode branchNode) {
                return compileBranch(branchNode, branchPath);
            }
            if (node instanceof CopyTaskNode copyTaskNode) {
                return compileCopyTask(copyTaskNode, branchPath);
            }
            throw new IllegalArgumentException("M1 编译器暂不支持节点：" + node.type());
        }

        private BpmnFragment compileCopyTask(CopyTaskNode node, List<String> branchPath) {
            JSONObject authored = toAuthoredJson(node);
            JSONObject compiled = new JSONObject(true);
            compiled.putAll(authored);
            compiled.put("branchPath", branchPath);
            compiled.put("startRuleJson", startRuleJson);
            compiled.put("variableMappingJson", variableMappingJson);
            CompiledNodeSnapshot snapshot = new CompiledNodeSnapshot(
                    node.nodeKey(),
                    node.type().name(),
                    node.name(),
                    ++snapshotOrder,
                    authored.toJSONString(),
                    compiled.toJSONString()
            );
            String element = "<serviceTask id=\"" + escapeXml(node.nodeKey())
                    + "\" name=\"" + escapeXml(node.name())
                    + "\" flowable:delegateExpression=\"${hunyuanCopyTaskDelegate}\">"
                    + "<extensionElements><flowable:field name=\"copyNodeKey\" stringValue=\""
                    + escapeXml(node.nodeKey())
                    + "\"/></extensionElements></serviceTask>";
            return new BpmnFragment(
                    List.of(node.nodeKey()),
                    List.of(node.nodeKey()),
                    List.of(element),
                    List.of(),
                    List.of(snapshot),
                    Set.of("COPY_TASK")
            );
        }

        private BpmnFragment compileHumanTask(HumanTaskNode node, List<String> branchPath) {
            JSONObject authored = toAuthoredJson(node);
            JSONArray employeeIds = authored.getJSONArray("employeeIds");
            boolean multiple = node.type() == ProcessNodeType.USER_TASK
                    && "EMPLOYEE".equalsIgnoreCase(node.candidateResolverType())
                    && employeeIds != null
                    && !employeeIds.isEmpty()
                    && ("sequential".equalsIgnoreCase(node.approvalMode())
                    || "parallelAll".equalsIgnoreCase(node.approvalMode()));
            if (!multiple) {
                return singleHumanTaskFragment(node, node.nodeKey(), node.name(), authored, branchPath, null, null);
            }

            List<BpmnFragment> memberFragments = new ArrayList<>();
            for (int index = 0; index < employeeIds.size(); index++) {
                JSONObject memberExtra = new JSONObject();
                memberExtra.put("employeeId", employeeIds.getLongValue(index));
                memberExtra.put("authoredNodeKey", node.nodeKey());
                memberExtra.put("authoredNodeName", node.name());
                memberExtra.put("approvalGroupKey", node.nodeKey());
                memberExtra.put("approvalGroupName", node.name());
                if ("parallelAll".equalsIgnoreCase(node.approvalMode())) {
                    memberExtra.put("parallelIndex", index + 1);
                    memberExtra.put("parallelTotal", employeeIds.size());
                } else {
                    memberExtra.put("sequentialIndex", index + 1);
                    memberExtra.put("sequentialTotal", employeeIds.size());
                }
                memberFragments.add(singleHumanTaskFragment(
                        node,
                        node.nodeKey() + "_" + (index + 1),
                        node.name() + "（" + (index + 1) + "/" + employeeIds.size() + "）",
                        authored,
                        branchPath,
                        memberExtra,
                        node.nodeKey()
                ));
            }
            if (!"parallelAll".equalsIgnoreCase(node.approvalMode())) {
                return composer.compose(memberFragments, idFactory);
            }

            String splitId = idFactory.splitGatewayId(node.nodeKey());
            String joinId = idFactory.joinGatewayId(node.nodeKey());
            List<String> elements = new ArrayList<>();
            elements.add("<parallelGateway id=\"" + escapeXml(splitId) + "\" name=\"并行分叉\"/>");
            elements.add("<parallelGateway id=\"" + escapeXml(joinId) + "\" name=\"并行汇聚\"/>");
            List<BpmnSequenceFlow> flows = new ArrayList<>();
            List<CompiledNodeSnapshot> snapshots = new ArrayList<>();
            for (BpmnFragment member : memberFragments) {
                elements.addAll(member.generatedElements());
                flows.addAll(member.sequenceFlows());
                snapshots.addAll(member.compiledNodeSnapshots());
                flows.add(new BpmnSequenceFlow(idFactory.nextFlowId(splitId, member.entryElementIds().get(0)), splitId, member.entryElementIds().get(0), null));
                flows.add(new BpmnSequenceFlow(idFactory.nextFlowId(member.exitElementIds().get(0), joinId), member.exitElementIds().get(0), joinId, null));
            }
            return new BpmnFragment(List.of(splitId), List.of(joinId), elements, flows, snapshots, Set.of());
        }

        private BpmnFragment singleHumanTaskFragment(
                HumanTaskNode node,
                String compiledKey,
                String compiledName,
                JSONObject authored,
                List<String> branchPath,
                JSONObject extra,
                String approvalGroupKey
        ) {
            JSONObject compiled = new JSONObject(true);
            compiled.putAll(authored);
            compiled.put("nodeKey", compiledKey);
            compiled.put("name", compiledName);
            compiled.put("type", node.type().name());
            compiled.put("authoredNodeKey", node.nodeKey());
            compiled.put("branchPath", branchPath);
            compiled.put("startRuleJson", startRuleJson);
            compiled.put("variableMappingJson", variableMappingJson);
            if (extra != null) {
                compiled.putAll(extra);
            }
            CompiledNodeSnapshot snapshot = new CompiledNodeSnapshot(
                    compiledKey,
                    node.type().name(),
                    compiledName,
                    ++snapshotOrder,
                    authored.toJSONString(),
                    compiled.toJSONString()
            );
            String element = "<userTask id=\"" + escapeXml(compiledKey)
                    + "\" name=\"" + escapeXml(compiledName)
                    + "\" flowable:assignee=\"${assignee_" + escapeXml(compiledKey) + "}\"/>";
            return new BpmnFragment(
                    List.of(compiledKey),
                    List.of(compiledKey),
                    List.of(element),
                    List.of(),
                    List.of(snapshot),
                    Set.of("ASSIGNMENT")
            );
        }

        private BpmnFragment compileBranch(BranchNode node, List<String> parentBranchPath) {
            String splitId = idFactory.splitGatewayId(node.nodeKey());
            String joinId = idFactory.joinGatewayId(node.nodeKey());
            boolean routed = node.type() != ProcessNodeType.PARALLEL_BRANCH;
            String entryId = routed ? idFactory.routeDelegateId(node.nodeKey()) : splitId;
            List<String> elements = new ArrayList<>();
            List<BpmnSequenceFlow> flows = new ArrayList<>();
            List<CompiledNodeSnapshot> snapshots = new ArrayList<>();
            Set<String> requirements = new LinkedHashSet<>();
            String defaultFlowId = null;

            if (routed) {
                elements.add(buildRouteDelegate(node, entryId));
                flows.add(new BpmnSequenceFlow(idFactory.nextFlowId(entryId, splitId), entryId, splitId, null));
                requirements.add("ROUTE_DECISION");
            }

            for (ProcessBranch branch : node.branches()) {
                List<String> childPath = new ArrayList<>(parentBranchPath);
                childPath.add(node.nodeKey() + "." + branch.branchKey());
                BpmnFragment branchFragment = compileNodes(branch.nodes(), List.copyOf(childPath));
                elements.addAll(branchFragment.generatedElements());
                flows.addAll(branchFragment.sequenceFlows());
                snapshots.addAll(branchFragment.compiledNodeSnapshots());
                requirements.addAll(branchFragment.runtimeRequirements());

                String target = branchFragment.entryElementIds().isEmpty()
                        ? joinId
                        : branchFragment.entryElementIds().get(0);
                String outgoingFlowId = idFactory.nextFlowId(splitId, target);
                String condition = routed && !branch.defaultBranch()
                        ? "${execution.getVariable('route_" + node.nodeKey() + "_" + branch.branchKey() + "') == true}"
                        : null;
                flows.add(new BpmnSequenceFlow(outgoingFlowId, splitId, target, condition));
                if (branch.defaultBranch()) {
                    defaultFlowId = outgoingFlowId;
                }
                for (String branchExit : branchFragment.exitElementIds()) {
                    flows.add(new BpmnSequenceFlow(idFactory.nextFlowId(branchExit, joinId), branchExit, joinId, null));
                }
            }

            String gatewayTag = switch (node.type()) {
                case EXCLUSIVE_BRANCH -> "exclusiveGateway";
                case PARALLEL_BRANCH -> "parallelGateway";
                case INCLUSIVE_BRANCH -> "inclusiveGateway";
                default -> throw new IllegalArgumentException("非分支节点：" + node.type());
            };
            String defaultAttribute = defaultFlowId == null ? "" : " default=\"" + escapeXml(defaultFlowId) + "\"";
            elements.add("<" + gatewayTag + " id=\"" + escapeXml(splitId) + "\" name=\"" + escapeXml(node.name()) + "分叉\"" + defaultAttribute + "/>");
            elements.add("<" + gatewayTag + " id=\"" + escapeXml(joinId) + "\" name=\"" + escapeXml(node.name()) + "汇聚\"/>");

            JSONObject authored = toAuthoredJson(node);
            JSONObject compiled = new JSONObject(true);
            compiled.putAll(authored);
            compiled.put("routeDelegateId", routed ? entryId : null);
            compiled.put("splitGatewayId", splitId);
            compiled.put("joinGatewayId", joinId);
            compiled.put("branchPath", parentBranchPath);
            snapshots.add(0, new CompiledNodeSnapshot(
                    node.nodeKey(),
                    node.type().name(),
                    node.name(),
                    ++snapshotOrder,
                    authored.toJSONString(),
                    compiled.toJSONString()
            ));
            return new BpmnFragment(List.of(entryId), List.of(joinId), elements, flows, snapshots, requirements);
        }

        private String buildRouteDelegate(BranchNode node, String delegateId) {
            return "<serviceTask id=\"" + escapeXml(delegateId)
                    + "\" name=\"" + escapeXml(node.name())
                    + "路由决定\" flowable:delegateExpression=\"${hunyuanRouteDecisionDelegate}\">"
                    + "<extensionElements><flowable:field name=\"routeNodeKey\" stringValue=\""
                    + escapeXml(node.nodeKey())
                    + "\"/></extensionElements></serviceTask>";
        }

        private JSONObject toAuthoredJson(ProcessNode node) {
            if (node instanceof HumanTaskNode humanTaskNode) {
                JSONObject authored = new JSONObject(true);
                authored.putAll(humanTaskNode.configuration());
                authored.put("nodeKey", node.nodeKey());
                authored.put("name", node.name());
                authored.put("type", node.type().name());
                return authored;
            }
            if (node instanceof CopyTaskNode copyTaskNode) {
                JSONObject authored = new JSONObject(true);
                authored.putAll(copyTaskNode.configuration());
                authored.put("nodeKey", node.nodeKey());
                authored.put("name", node.name());
                authored.put("type", node.type().name());
                return authored;
            }
            BranchNode branchNode = (BranchNode) node;
            JSONObject authored = new JSONObject(true);
            authored.put("nodeKey", node.nodeKey());
            authored.put("name", node.name());
            authored.put("type", node.type().name());
            JSONArray branches = new JSONArray();
            for (ProcessBranch branch : branchNode.branches()) {
                JSONObject branchJson = new JSONObject(true);
                branchJson.put("branchKey", branch.branchKey());
                branchJson.put("name", branch.name());
                branchJson.put("isDefault", branch.defaultBranch());
                branchJson.put("condition", branch.condition());
                branchJson.put("nodes", branch.nodes().stream().map(this::toAuthoredJson).toList());
                branches.add(branchJson);
            }
            authored.put("branches", branches);
            return authored;
        }
    }

    private String buildBpmnXml(String modelKey, String modelName, List<LegacyBpmnFragment> fragments) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xmlBuilder.append("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" ");
        xmlBuilder.append("xmlns:flowable=\"http://flowable.org/bpmn\" ");
        xmlBuilder.append("targetNamespace=\"http://hunyuan.sa/bpm\">");
        xmlBuilder.append("<process id=\"").append(escapeXml(modelKey)).append("\" ");
        xmlBuilder.append("name=\"").append(escapeXml(modelName)).append("\" isExecutable=\"true\">");
        xmlBuilder.append("<startEvent id=\"startEvent\" name=\"开始\"/>");

        String previousRef = "startEvent";
        int flowIndex = 0;
        for (LegacyBpmnFragment fragment : fragments) {
            if (fragment instanceof UserTaskFragment userTaskFragment) {
                UserTaskNode task = userTaskFragment.task();
                appendUserTask(xmlBuilder, task);
                appendSequenceFlow(xmlBuilder, "flow_" + flowIndex++, previousRef, task.nodeKey());
                previousRef = task.nodeKey();
                continue;
            }

            ParallelAllFragment parallel = (ParallelAllFragment) fragment;
            xmlBuilder.append("<parallelGateway id=\"").append(escapeXml(parallel.splitGatewayKey()))
                    .append("\" name=\"并行分叉\"/>");
            xmlBuilder.append("<parallelGateway id=\"").append(escapeXml(parallel.joinGatewayKey()))
                    .append("\" name=\"并行汇聚\"/>");
            appendSequenceFlow(xmlBuilder, "flow_" + flowIndex++, previousRef, parallel.splitGatewayKey());
            for (UserTaskNode memberTask : parallel.memberTasks()) {
                appendUserTask(xmlBuilder, memberTask);
                appendSequenceFlow(
                        xmlBuilder,
                        "flow_" + flowIndex++,
                        parallel.splitGatewayKey(),
                        memberTask.nodeKey()
                );
                appendSequenceFlow(
                        xmlBuilder,
                        "flow_" + flowIndex++,
                        memberTask.nodeKey(),
                        parallel.joinGatewayKey()
                );
            }
            previousRef = parallel.joinGatewayKey();
        }

        xmlBuilder.append("<endEvent id=\"endEvent\" name=\"结束\"/>");
        appendSequenceFlow(xmlBuilder, "flow_end", previousRef, "endEvent");
        xmlBuilder.append("</process>");
        xmlBuilder.append("</definitions>");
        return xmlBuilder.toString();
    }

    private void appendUserTask(StringBuilder xmlBuilder, UserTaskNode taskNode) {
        xmlBuilder.append("<userTask id=\"").append(escapeXml(taskNode.nodeKey())).append("\" ");
        xmlBuilder.append("name=\"").append(escapeXml(taskNode.nodeName())).append("\" ");
        xmlBuilder.append("flowable:assignee=\"${assignee_")
                .append(escapeXml(taskNode.nodeKey()))
                .append("}\"/>");
    }

    private void appendSequenceFlow(
            StringBuilder xmlBuilder,
            String flowId,
            String sourceRef,
            String targetRef
    ) {
        xmlBuilder.append("<sequenceFlow id=\"").append(escapeXml(flowId))
                .append("\" sourceRef=\"").append(escapeXml(sourceRef))
                .append("\" targetRef=\"").append(escapeXml(targetRef))
                .append("\"/>");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private sealed interface LegacyBpmnFragment permits UserTaskFragment, ParallelAllFragment {
    }

    private record UserTaskFragment(UserTaskNode task) implements LegacyBpmnFragment {
    }

    private record ParallelAllFragment(
            String splitGatewayKey,
            List<UserTaskNode> memberTasks,
            String joinGatewayKey
    ) implements LegacyBpmnFragment {
    }

    private record UserTaskNode(String nodeKey, String nodeName) {
    }
}
