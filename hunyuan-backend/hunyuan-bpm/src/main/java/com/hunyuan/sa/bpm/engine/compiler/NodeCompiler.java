package com.hunyuan.sa.bpm.engine.compiler;

import com.hunyuan.sa.bpm.engine.ast.ProcessNode;
import com.hunyuan.sa.bpm.engine.ast.ProcessNodeType;

/**
 * 单类 AST 节点的片段编译扩展点。
 */
public interface NodeCompiler<T extends ProcessNode> {

    ProcessNodeType supports();

    BpmnFragment compile(T node, CompilerContext context);
}
