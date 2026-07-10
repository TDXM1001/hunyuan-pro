package com.hunyuan.sa.bpm.engine.compiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * SimpleModel -> BPMN XML 的最小编译器。
 */
@Component
public class SimpleModelBpmnCompiler {

    /**
     * 将当前设计器草稿编译为顺序审批 BPMN XML 与节点快照。
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
        List<UserTaskNode> userTaskNodes = new ArrayList<>();

        if (nodes != null) {
            for (int index = 0; index < nodes.size(); index++) {
                JSONObject nodeObject = nodes.getJSONObject(index);
                if (nodeObject == null) {
                    continue;
                }

                String nodeKey = firstNonBlank(
                        nodeObject.getString("nodeKey"),
                        nodeObject.getString("id"),
                        "task_" + (index + 1)
                );
                String nodeType = firstNonBlank(nodeObject.getString("type"), "userTask");
                String nodeName = firstNonBlank(nodeObject.getString("name"), "审批节点" + (index + 1));

                if (isSequentialEmployeeApproval(nodeObject, nodeType)) {
                    JSONArray employeeIds = nodeObject.getJSONArray("employeeIds");
                    int sequentialTotal = employeeIds.size();
                    for (int sequentialIndex = 0; sequentialIndex < sequentialTotal; sequentialIndex++) {
                        String expandedNodeKey = nodeKey + "_" + (sequentialIndex + 1);
                        String expandedNodeName = nodeName + "（" + (sequentialIndex + 1) + "/" + sequentialTotal + "）";
                        JSONObject compiledNodeObject = buildCompiledNodeObject(
                                nodeObject,
                                expandedNodeKey,
                                nodeType,
                                expandedNodeName,
                                startRuleJson,
                                variableMappingJson
                        );
                        compiledNodeObject.put("employeeId", employeeIds.getLongValue(sequentialIndex));
                        compiledNodeObject.put("authoredNodeKey", nodeKey);
                        compiledNodeObject.put("authoredNodeName", nodeName);
                        compiledNodeObject.put("sequentialIndex", sequentialIndex + 1);
                        compiledNodeObject.put("sequentialTotal", sequentialTotal);

                        nodeSnapshots.add(new CompiledNodeSnapshot(
                                expandedNodeKey,
                                nodeType,
                                expandedNodeName,
                                nodeSnapshots.size() + 1,
                                JSON.toJSONString(nodeObject),
                                JSON.toJSONString(compiledNodeObject)
                        ));
                        userTaskNodes.add(new UserTaskNode(expandedNodeKey, expandedNodeName));
                    }
                    continue;
                }

                JSONObject compiledNodeObject = buildCompiledNodeObject(
                        nodeObject,
                        nodeKey,
                        nodeType,
                        nodeName,
                        startRuleJson,
                        variableMappingJson
                );
                nodeSnapshots.add(new CompiledNodeSnapshot(
                        nodeKey,
                        nodeType,
                        nodeName,
                        nodeSnapshots.size() + 1,
                        JSON.toJSONString(nodeObject),
                        JSON.toJSONString(compiledNodeObject)
                ));

                if ("userTask".equals(nodeType)) {
                    userTaskNodes.add(new UserTaskNode(nodeKey, nodeName));
                }
            }
        }

        return new CompiledDefinitionArtifact(
                buildSequentialBpmnXml(modelKey, modelName, userTaskNodes),
                nodeSnapshots
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
        compiledNodeObject.put("candidateResolverType", firstNonBlank(
                nodeObject.getString("candidateResolverType"),
                nodeObject.getString("resolverType")
        ));
        compiledNodeObject.put("listeners", nodeObject.get("listeners"));
        compiledNodeObject.put("variableMappingJson", variableMappingJson);
        compiledNodeObject.put("startRuleJson", startRuleJson);
        return compiledNodeObject;
    }

    private boolean isSequentialEmployeeApproval(JSONObject nodeObject, String nodeType) {
        return "userTask".equals(nodeType)
                && "sequential".equalsIgnoreCase(nodeObject.getString("approvalMode"))
                && "EMPLOYEE".equalsIgnoreCase(firstNonBlank(
                nodeObject.getString("candidateResolverType"),
                nodeObject.getString("resolverType")
        ))
                && nodeObject.getJSONArray("employeeIds") != null
                && !nodeObject.getJSONArray("employeeIds").isEmpty();
    }

    private String buildSequentialBpmnXml(String modelKey, String modelName, List<UserTaskNode> userTaskNodes) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xmlBuilder.append("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" ");
        xmlBuilder.append("xmlns:flowable=\"http://flowable.org/bpmn\" ");
        xmlBuilder.append("targetNamespace=\"http://hunyuan.sa/bpm\">");
        xmlBuilder.append("<process id=\"").append(escapeXml(modelKey)).append("\" ");
        xmlBuilder.append("name=\"").append(escapeXml(modelName)).append("\" isExecutable=\"true\">");
        xmlBuilder.append("<startEvent id=\"startEvent\" name=\"开始\"/>");

        String previousRef = "startEvent";
        for (int index = 0; index < userTaskNodes.size(); index++) {
            UserTaskNode taskNode = userTaskNodes.get(index);
            xmlBuilder.append("<userTask id=\"").append(escapeXml(taskNode.nodeKey())).append("\" ");
            xmlBuilder.append("name=\"").append(escapeXml(taskNode.nodeName())).append("\" ");
            xmlBuilder.append("flowable:assignee=\"${assignee_").append(escapeXml(taskNode.nodeKey())).append("}\"/>");

            String flowId = "flow_" + index;
            xmlBuilder.append("<sequenceFlow id=\"").append(flowId).append("\" sourceRef=\"")
                    .append(previousRef).append("\" targetRef=\"").append(escapeXml(taskNode.nodeKey())).append("\"/>");
            previousRef = taskNode.nodeKey();
        }

        xmlBuilder.append("<endEvent id=\"endEvent\" name=\"结束\"/>");
        xmlBuilder.append("<sequenceFlow id=\"flow_end\" sourceRef=\"")
                .append(previousRef).append("\" targetRef=\"endEvent\"/>");
        xmlBuilder.append("</process>");
        xmlBuilder.append("</definitions>");
        return xmlBuilder.toString();
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

    private record UserTaskNode(String nodeKey, String nodeName) {
    }
}
