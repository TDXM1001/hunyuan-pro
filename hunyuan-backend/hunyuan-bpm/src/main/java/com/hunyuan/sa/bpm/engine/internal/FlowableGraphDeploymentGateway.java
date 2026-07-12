package com.hunyuan.sa.bpm.engine.internal;

import jakarta.annotation.Resource;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Component;

@Component
public class FlowableGraphDeploymentGateway implements GraphFlowableDeploymentGateway {
    @Resource private RepositoryService repositoryService;
    public GraphFlowableDeployment deploy(String key, String name, String xml) {
        Deployment deployment = repositoryService.createDeployment().name(name).category("hunyuan-bpm").addString(key + ".bpmn20.xml", xml).deploy();
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        if (definition == null) { repositoryService.deleteDeployment(deployment.getId(), true); throw new IllegalStateException("Flowable 部署后未找到流程定义：" + key); }
        return new GraphFlowableDeployment(deployment.getId(), definition.getId());
    }
    public void delete(GraphFlowableDeployment deployment) { repositoryService.deleteDeployment(deployment.deploymentId(), true); }
    public void suspend(String processDefinitionId) { repositoryService.suspendProcessDefinitionById(processDefinitionId); }
}
