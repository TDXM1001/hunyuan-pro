package com.hunyuan.sa.bpm.engine.internal;

import jakarta.annotation.Resource;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Component;

/**
 * Flowable 流程定义部署网关，仅在 BPM 模块内部使用。
 */
@Component
public class FlowableProcessDefinitionGateway {

    @Resource
    private RepositoryService repositoryService;

    /**
     * 部署编译后的 BPMN XML，并返回 Flowable 内部定义 ID。
     */
    public String deploy(String modelKey, String modelName, String compiledBpmnXml) {
        Deployment deployment = repositoryService.createDeployment()
                .name(modelName)
                .category("hunyuan-bpm")
                .addString(modelKey + ".bpmn20.xml", compiledBpmnXml)
                .deploy();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        if (processDefinition == null) {
            throw new IllegalStateException("Flowable 部署成功后未找到流程定义：" + modelKey);
        }
        return processDefinition.getId();
    }
}
