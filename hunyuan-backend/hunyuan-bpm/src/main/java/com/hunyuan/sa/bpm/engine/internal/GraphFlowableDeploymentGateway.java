package com.hunyuan.sa.bpm.engine.internal;

public interface GraphFlowableDeploymentGateway {
    GraphFlowableDeployment deploy(String processKey, String processName, String bpmnXml);
    void delete(GraphFlowableDeployment deployment);
    void suspend(String processDefinitionId);
}
