package com.hunyuan.sa.bpm.engine.compiler;

import java.util.List;

/**
 * SimpleModel 编译后的定义产物。
 */
public record CompiledDefinitionArtifact(
        String compiledBpmnXml,
        List<CompiledNodeSnapshot> nodeSnapshots
) {
}
