package com.hunyuan.sa.bpm.api.business.domain;

import lombok.Data;

/**
 * 业务模块发起流程命令。
 */
@Data
public class BpmBusinessStartCommand {

    private String businessType;

    private Long businessId;

    private String businessKey;

    private String definitionKey;

    private Long startEmployeeId;

    private String formDataJson;

    private String title;

    private String summary;
}
