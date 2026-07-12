package com.hunyuan.sa.bpm.module.definition.domain.form;
import jakarta.validation.constraints.NotNull; import lombok.Data;
@Data public class GraphDefinitionPublishForm { @NotNull(message="Graph 草稿ID不能为空") private Long draftId; }
