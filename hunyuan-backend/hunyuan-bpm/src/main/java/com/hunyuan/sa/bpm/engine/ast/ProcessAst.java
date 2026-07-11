package com.hunyuan.sa.bpm.engine.ast;

import java.util.List;

/**
 * 已解析的 Hunyuan 流程模型。
 */
public record ProcessAst(
        int schemaVersion,
        int maxBranchDepth,
        List<ProcessNode> nodes
) {
    public ProcessAst {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
    }
}
