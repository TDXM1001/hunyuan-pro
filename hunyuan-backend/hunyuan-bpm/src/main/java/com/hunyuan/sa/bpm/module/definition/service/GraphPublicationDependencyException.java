package com.hunyuan.sa.bpm.module.definition.service;

/**
 * 发布前无法冻结跨模块引用时的失败关闭信号。
 */
public class GraphPublicationDependencyException extends IllegalStateException {

    public GraphPublicationDependencyException(String message) {
        super(message);
    }

    public GraphPublicationDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
