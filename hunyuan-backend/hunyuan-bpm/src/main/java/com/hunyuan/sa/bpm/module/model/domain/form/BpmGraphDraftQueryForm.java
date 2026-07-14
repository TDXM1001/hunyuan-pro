package com.hunyuan.sa.bpm.module.model.domain.form;

import com.hunyuan.sa.base.common.domain.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BpmGraphDraftQueryForm extends PageParam {

    private String processKey;

    private String processName;

    private Long categoryId;
}
