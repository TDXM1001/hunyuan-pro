package com.hunyuan.sa.bpm.module.businesscontract.domain.form;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class BpmBusinessObjectTechnicalDiffForm { @NotBlank private String contractKey; @NotNull @Min(1) private Integer leftVersion; @NotNull @Min(1) private Integer rightVersion; }
