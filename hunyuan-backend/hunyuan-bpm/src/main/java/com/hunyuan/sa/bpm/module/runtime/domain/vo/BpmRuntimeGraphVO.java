package com.hunyuan.sa.bpm.module.runtime.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 由 Hunyuan authored 结构和运行事实装配的流程图。
 */
@Data
public class BpmRuntimeGraphVO {

    private Long instanceId;
    private Long definitionId;
    private List<Node> nodes;
    private List<RouteDecision> routeDecisions;

    @Data
    public static class Node {
        private Long definitionNodeId;
        private String nodeKey;
        private String nodeName;
        private String nodeType;
        private Integer sortOrder;
        private List<String> branchPath;
        private String state;
    }

    @Data
    public static class RouteDecision {
        private Long routeDecisionId;
        private Long instanceId;
        private Long definitionId;
        private Long definitionNodeId;
        private String routeNodeKey;
        private Long inputFormDataVersion;
        private List<String> matchedBranchKeys;
        private Boolean defaultBranchUsed;
        private String evaluationStatus;
        private String reasonSnapshotJson;
        private LocalDateTime evaluatedAt;
    }
}
