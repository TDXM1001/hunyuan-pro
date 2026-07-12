package com.hunyuan.sa.bpm.engine.compiler.graph;

/**
 * 发布预检失败时阻止 Graph 编译。
 */
public class GraphCompilationException extends IllegalArgumentException {

    public GraphCompilationException(String message) {
        super(message);
    }
}
