package com.hunyuan.sa.bpm.module.operations.domain.form;

import lombok.Data;

/**
 * BPM 运营治理指标查询。
 */
@Data
public class BpmOperationsMetricQueryForm {

    private Long graphDefinitionVersionId;

    private String definitionNodeId;

    private Long organizationId;
}
