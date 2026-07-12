package com.hunyuan.sa.bpm.engine.graph;

/**
 * Graph 中节点与边的语义作用域。
 */
public record GraphScope(String scopeId, String parentScopeId, String name) {
}
