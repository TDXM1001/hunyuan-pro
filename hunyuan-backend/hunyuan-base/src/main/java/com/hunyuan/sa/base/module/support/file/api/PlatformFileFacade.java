package com.hunyuan.sa.base.module.support.file.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 平台文件能力公开边界。
 *
 * <p>业务模块只依赖该接口，不直接感知本地存储、云存储或历史文件服务实现。</p>
 */
public interface PlatformFileFacade {

    ResponseDTO<PlatformFileUploadResult> upload(
            MultipartFile file, Integer folderType, RequestUser requestUser);

    ResponseDTO<String> resolveUrl(String fileKey);

    /**
     * 分页读取已授权管理端可见的文件记录。
     */
    PageResult<PlatformFileSummary> queryPage(PlatformFilePageQuery query);

    /**
     * 按文件引用读取下载内容，调用方负责将结果写入 HTTP 响应流。
     */
    ResponseDTO<PlatformFileDownloadResult> download(String fileKey, String userAgent);
}
