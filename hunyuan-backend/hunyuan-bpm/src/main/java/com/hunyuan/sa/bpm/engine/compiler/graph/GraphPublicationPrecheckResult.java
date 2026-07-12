package com.hunyuan.sa.bpm.engine.compiler.graph;

import java.util.List;

/**
 * Graph 发布前的结构检查结果。
 */
public record GraphPublicationPrecheckResult(boolean pass, List<GraphPublicationFinding> findings) {
}
