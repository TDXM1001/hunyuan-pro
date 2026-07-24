package com.hunyuan.sa.base.module.support.file.api;

/**
 * 平台文件下载结果。
 *
 * <p>该对象只在服务边界和 HTTP 输出层之间传递，控制器不会将文件字节序列化为 JSON。</p>
 *
 * @param data 文件二进制内容
 * @param fileName 下载时展示的文件名称
 * @param fileSize 文件大小，单位为字节
 */
public record PlatformFileDownloadResult(byte[] data, String fileName, Long fileSize) {
}
