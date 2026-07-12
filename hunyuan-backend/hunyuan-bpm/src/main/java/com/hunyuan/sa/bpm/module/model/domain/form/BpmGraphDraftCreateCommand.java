package com.hunyuan.sa.bpm.module.model.domain.form;

import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;

/**
 * 创建正式 Graph 草稿的服务命令。
 */
public record BpmGraphDraftCreateCommand(
        String processKey,
        String processName,
        Long categoryId,
        HunyuanProcessDefinitionGraph graph,
        Long operatorEmployeeId
) {
}
