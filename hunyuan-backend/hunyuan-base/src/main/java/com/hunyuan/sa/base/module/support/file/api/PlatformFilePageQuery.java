package com.hunyuan.sa.base.module.support.file.api;

import com.hunyuan.sa.base.common.domain.PageParam;
import com.hunyuan.sa.base.common.swagger.SchemaEnum;
import com.hunyuan.sa.base.common.validator.enumeration.CheckEnum;
import com.hunyuan.sa.base.module.support.file.constant.FileFolderTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 平台文件分页查询条件。
 *
 * <p>字段语义与历史文件查询保持一致，避免稳定管理接口改变已有筛选结果。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformFilePageQuery extends PageParam {

    @SchemaEnum(value = FileFolderTypeEnum.class, desc = "文件夹类型")
    @CheckEnum(value = FileFolderTypeEnum.class, message = "文件夹类型错误")
    private Integer folderType;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "文件引用")
    private String fileKey;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "创建人名称")
    private String creatorName;

    @Schema(description = "创建开始日期")
    private LocalDate createTimeBegin;

    @Schema(description = "创建结束日期")
    private LocalDate createTimeEnd;
}
