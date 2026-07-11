package com.hunyuan.sa.bpm.engine.ast;

/**
 * 受控流程 AST 节点。
 */
public sealed interface ProcessNode permits HumanTaskNode, CopyTaskNode, BranchNode {

    String nodeKey();

    String name();

    ProcessNodeType type();
}
