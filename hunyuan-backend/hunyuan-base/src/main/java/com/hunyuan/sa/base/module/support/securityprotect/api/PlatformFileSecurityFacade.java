package com.hunyuan.sa.base.module.support.securityprotect.api;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传安全校验公开边界。
 *
 * <p>文件 owner 只提交待校验内容，不感知安全基线配置或 MIME 检测实现。</p>
 */
public interface PlatformFileSecurityFacade {

    ResponseDTO<String> validateUpload(MultipartFile file);
}
