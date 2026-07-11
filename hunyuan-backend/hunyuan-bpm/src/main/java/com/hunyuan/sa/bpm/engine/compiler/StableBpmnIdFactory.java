package com.hunyuan.sa.bpm.engine.compiler;

/**
 * 为编译器生成稳定、可校验但不承载业务推断的 BPMN ID。
 */
public class StableBpmnIdFactory {

    private int flowSequence;

    public String nextFlowId(String sourceRef, String targetRef) {
        return "hy_flow_" + sanitize(sourceRef) + "__" + sanitize(targetRef) + "__" + flowSequence++;
    }

    public String routeDelegateId(String nodeKey) {
        return "hy_route_" + nodeKey + "_decide";
    }

    public String splitGatewayId(String nodeKey) {
        return "hy_gateway_" + nodeKey + "_split";
    }

    public String joinGatewayId(String nodeKey) {
        return "hy_gateway_" + nodeKey + "_join";
    }

    private String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9_]", "_");
    }
}
