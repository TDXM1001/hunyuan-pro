package com.hunyuan.sa.bpm.module.evolution.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BpmAffectedInstanceVO {
    private Long instanceId;
    private String instanceNo;
    private Long graphDefinitionVersionId;
    private String definitionKeySnapshot;
    private String title;
    private String businessKey;
    private Integer runState;
    private Integer activeTaskCount;
    private LocalDateTime startedAt;
}
