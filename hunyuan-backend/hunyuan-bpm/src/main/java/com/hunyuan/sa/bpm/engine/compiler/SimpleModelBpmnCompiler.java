package com.hunyuan.sa.bpm.engine.compiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
        JSONArray nodes = simpleModelObject.getJSONArray("nodes");
        List<CompiledNodeSnapshot> nodeSnapshots = new ArrayList<>();
        List<BpmnFragment> fragments = new ArrayList<>();

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
            List<BpmnFragment> fragments
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

    private String buildBpmnXml(String modelKey, String modelName, List<BpmnFragment> fragments) {
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
        for (BpmnFragment fragment : fragments) {
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

    private sealed interface BpmnFragment permits UserTaskFragment, ParallelAllFragment {
    }

    private record UserTaskFragment(UserTaskNode task) implements BpmnFragment {
    }

    private record ParallelAllFragment(
            String splitGatewayKey,
            List<UserTaskNode> memberTasks,
            String joinGatewayKey
    ) implements BpmnFragment {
    }

    private record UserTaskNode(String nodeKey, String nodeName) {
    }
}
