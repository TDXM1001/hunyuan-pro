package com.hunyuan.sa.base.module.support.file.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台文件管理摘要，仅暴露管理端列表展示所需字段。
 */
@Data
public class PlatformFileSummary {

    @Schema(description = "文件记录ID")
    private Long fileId;

    @Schema(description = "存储文件夹类型")
    private Integer folderType;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件大小，单位为字节")
    private Integer fileSize;

    @Schema(description = "文件扩展名")
    private String fileType;

    @Schema(description = "持久化文件引用")
    private String fileKey;

    @Schema(description = "创建人ID")
    private Long creatorId;

    @Schema(description = "创建人名称")
    private String creatorName;

    @Schema(description = "创建人类型")
    private Integer creatorUserType;

    @Schema(description = "当前文件访问地址")
    private String fileUrl;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
