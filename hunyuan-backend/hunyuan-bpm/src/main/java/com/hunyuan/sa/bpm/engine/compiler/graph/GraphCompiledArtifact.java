package com.hunyuan.sa.bpm.engine.compiler.graph;

import java.util.List;

/**
 * 新 Graph 编译后的 BPMN 与完整反查映射。
 */
public record GraphCompiledArtifact(
        String compiledBpmnXml,
        List<GraphCompiledElementMapping> mappings
) {
}
