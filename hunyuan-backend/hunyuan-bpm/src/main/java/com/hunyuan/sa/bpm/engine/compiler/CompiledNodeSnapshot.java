package com.hunyuan.sa.bpm.engine.compiler;

/**
 * 编译后的节点快照。
 */
public record CompiledNodeSnapshot(
        String nodeKey,
        String nodeType,
        String nodeNameSnapshot,
        Integer sortOrder,
        String authoredRuleSnapshotJson,
        String compiledNodeSnapshotJson
) {
}
