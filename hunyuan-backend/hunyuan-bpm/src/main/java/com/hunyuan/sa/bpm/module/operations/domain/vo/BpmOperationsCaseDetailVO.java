package com.hunyuan.sa.bpm.module.operations.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * BPM 运营治理工单详情与追加式处置审计。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmOperationsCaseDetailVO extends BpmOperationsCaseVO {

    private List<BpmOperationsActionLogVO> actionLogs;
}
