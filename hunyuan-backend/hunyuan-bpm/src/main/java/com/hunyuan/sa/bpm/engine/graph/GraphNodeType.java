package com.hunyuan.sa.bpm.engine.graph;

/**
 * 正式流程图的节点类型。M1 只允许核心建模节点发布。
 */
public enum GraphNodeType {
    START,
    END,
    APPROVAL,
    HANDLE,
    COPY,
    CONDITION,
    PARALLEL_GATEWAY,
    INCLUSIVE_GATEWAY,
    DELAY,
    EXTERNAL_TRIGGER,
    SUB_PROCESS
}
