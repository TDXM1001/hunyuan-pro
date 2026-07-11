package com.hunyuan.sa.bpm.engine.compiler;

import com.hunyuan.sa.bpm.engine.ast.BranchNode;
import com.hunyuan.sa.bpm.engine.ast.ProcessAst;
import com.hunyuan.sa.bpm.engine.ast.ProcessBranch;
import com.hunyuan.sa.bpm.engine.ast.ProcessNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 按 authored 顺序深度优先遍历流程 AST。
 */
public class ProcessAstWalker {

    public List<ProcessNode> walk(ProcessAst ast) {
        List<ProcessNode> result = new ArrayList<>();
        if (ast != null) {
            append(ast.nodes(), result);
        }
        return List.copyOf(result);
    }

    private void append(List<ProcessNode> nodes, List<ProcessNode> target) {
        for (ProcessNode node : nodes) {
            target.add(node);
            if (node instanceof BranchNode branchNode) {
                for (ProcessBranch branch : branchNode.branches()) {
                    append(branch.nodes(), target);
                }
            }
        }
    }
}
