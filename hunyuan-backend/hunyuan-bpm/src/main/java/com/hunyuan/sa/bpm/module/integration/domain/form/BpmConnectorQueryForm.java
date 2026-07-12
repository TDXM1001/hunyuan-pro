package com.hunyuan.sa.bpm.module.integration.domain.form;

import com.hunyuan.sa.base.common.domain.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 登记连接器分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmConnectorQueryForm extends PageParam {
    private String connectorKey;
    private String connectorName;
    private String enabledState;
}
