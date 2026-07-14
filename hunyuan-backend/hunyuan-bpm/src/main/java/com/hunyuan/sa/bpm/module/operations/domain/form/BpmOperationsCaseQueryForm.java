package com.hunyuan.sa.bpm.module.operations.domain.form;

import com.hunyuan.sa.base.common.domain.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * BPM 运营治理异常队列查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmOperationsCaseQueryForm extends PageParam {

    private String businessKey;

    private Long graphDefinitionVersionId;

    private Long assigneeEmployeeId;

    private String caseStatus;

    private String slaLevel;

    private String failureCode;

    private String eventId;

    private Long organizationId;

    private String definitionNodeId;
}
