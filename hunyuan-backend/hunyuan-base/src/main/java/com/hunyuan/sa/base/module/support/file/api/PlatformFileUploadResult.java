package com.hunyuan.sa.base.module.support.file.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 平台文件上传结果。
 *
 * @param fileId 文件记录主键
 * @param fileName 原始文件名
 * @param fileUrl 当前可访问地址
 * @param fileKey 持久化文件引用
 * @param fileSize 文件大小，单位为字节
 * @param fileType 文件扩展名
 */
public record PlatformFileUploadResult(
        @Schema(description = "文件记录ID") Long fileId,
        @Schema(description = "原始文件名") String fileName,
        @Schema(description = "当前可访问地址") String fileUrl,
        @Schema(description = "持久化文件引用") String fileKey,
        @Schema(description = "文件大小，单位为字节") Long fileSize,
        @Schema(description = "文件扩展名") String fileType
) {
}
