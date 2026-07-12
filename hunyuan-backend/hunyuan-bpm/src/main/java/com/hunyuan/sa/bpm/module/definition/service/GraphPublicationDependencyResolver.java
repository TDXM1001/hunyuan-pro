package com.hunyuan.sa.bpm.module.definition.service;

import com.hunyuan.sa.bpm.engine.graph.HunyuanProcessDefinitionGraph;

/**
 * M1 在发布期读取 M2/M3 目录并生成不可伪造的依赖快照。
 */
public interface GraphPublicationDependencyResolver {

    GraphPublicationDependencySnapshot resolve(HunyuanProcessDefinitionGraph graph);
}
