package com.hunyuan.sa.bpm.engine.ast;

import java.util.List;

/**
 * 固定分叉与汇合的受控分支节点。
 */
public record BranchNode(
        String nodeKey,
        String name,
        ProcessNodeType type,
        List<ProcessBranch> branches
) implements ProcessNode {
    public BranchNode {
        branches = branches == null ? List.of() : List.copyOf(branches);
    }
}
