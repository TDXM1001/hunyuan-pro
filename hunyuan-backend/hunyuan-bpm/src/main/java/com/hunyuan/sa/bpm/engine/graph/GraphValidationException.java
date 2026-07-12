package com.hunyuan.sa.bpm.engine.graph;

/**
 * 作者 Graph 的局部结构错误。
 */
public class GraphValidationException extends IllegalArgumentException {

    public GraphValidationException(String message) {
        super(message);
    }
}
