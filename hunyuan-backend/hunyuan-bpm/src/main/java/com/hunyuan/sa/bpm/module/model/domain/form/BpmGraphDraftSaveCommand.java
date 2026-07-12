package com.hunyuan.sa.bpm.module.model.domain.form;

import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;

/**
 * 通过 revision 条件保存正式 Graph 草稿的服务命令。
 */
public record BpmGraphDraftSaveCommand(
        Long draftId,
        int expectedRevision,
        HunyuanProcessDefinitionGraph graph,
        Long operatorEmployeeId
) {
}
