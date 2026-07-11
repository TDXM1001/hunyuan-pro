package com.hunyuan.sa.bpm.engine.ast;

import java.util.List;

/**
 * 分支节点中的一条 authored 分支。
 */
public record ProcessBranch(
        String branchKey,
        String name,
        boolean defaultBranch,
        RouteCondition condition,
        List<ProcessNode> nodes
) {
    public ProcessBranch {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
    }
}
