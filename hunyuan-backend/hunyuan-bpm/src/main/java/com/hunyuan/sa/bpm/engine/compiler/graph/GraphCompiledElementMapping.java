package com.hunyuan.sa.bpm.engine.compiler.graph;

/**
 * 受控 BPMN 元素与 Graph 作者元素之间的反查映射。
 */
public record GraphCompiledElementMapping(
        String authoredElementId,
        String authoredElementKind,
        String compiledElementId,
        String compiledElementType
) {
}
