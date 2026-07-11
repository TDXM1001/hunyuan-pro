package com.hunyuan.sa.bpm.engine.compiler;

import com.hunyuan.sa.bpm.engine.ast.ProcessNode;

import java.util.List;
import java.util.function.Function;

/**
 * 编译期间共享稳定 ID、快照序号和递归片段入口。
 */
public class CompilerContext {

    private final StableBpmnIdFactory idFactory;
    private final Function<List<ProcessNode>, BpmnFragment> nestedCompiler;
    private int snapshotSequence;

    public CompilerContext(
            StableBpmnIdFactory idFactory,
            Function<List<ProcessNode>, BpmnFragment> nestedCompiler
    ) {
        this.idFactory = idFactory;
        this.nestedCompiler = nestedCompiler;
    }

    public StableBpmnIdFactory idFactory() {
        return idFactory;
    }

    public BpmnFragment compileNodes(List<ProcessNode> nodes) {
        return nestedCompiler.apply(nodes);
    }

    public int nextSnapshotOrder() {
        return ++snapshotSequence;
    }
}
