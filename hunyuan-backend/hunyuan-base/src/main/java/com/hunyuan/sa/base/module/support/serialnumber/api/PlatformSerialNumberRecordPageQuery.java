package com.hunyuan.sa.base.module.support.serialnumber.api;

import com.hunyuan.sa.base.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台序列号生成记录分页查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformSerialNumberRecordPageQuery extends PageParam {

    @Schema(description = "序列号定义标识")
    @NotNull(message = "序列号定义标识不能为空")
    private Integer serialNumberId;
}
