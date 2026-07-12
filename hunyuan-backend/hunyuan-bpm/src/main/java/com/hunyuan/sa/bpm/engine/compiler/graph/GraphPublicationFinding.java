package com.hunyuan.sa.bpm.engine.compiler.graph;

/**
 * Graph 发布预检定位到的结构化问题。
 */
public record GraphPublicationFinding(
        String code,
        String message,
        String authoredElementId,
        String fixHint
) {
}
