package com.hunyuan.sa.bpm.module.definition.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 发布后不可变 Graph 定义的只读检查视图。
 */
@Data
public class GraphDefinitionDetailVO {

    private Long graphDefinitionVersionId;

    private String processKey;

    private Integer definitionVersion;

    private String lifecycleState;

    private String graphSnapshotJson;

    private String layoutSnapshotJson;

    private String semanticHash;

    private String dependencyVersionsJson;

    private String compilerVersion;

    private String compiledBpmnXml;

    private String deploymentId;

    private String engineProcessDefinitionId;

    private Long publishedByEmployeeId;

    private LocalDateTime publishedAt;

    private List<GraphDefinitionElementMappingVO> mappings;
}
