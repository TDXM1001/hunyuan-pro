package com.hunyuan.sa.bpm.module.definition.domain.vo;

import lombok.Data;

/**
 * 已发布 Graph 的 authored 元素与编译元素映射。
 */
@Data
public class GraphDefinitionElementMappingVO {

    private String authoredElementId;

    private String authoredElementKind;

    private String compiledElementId;

    private String compiledElementType;
}
