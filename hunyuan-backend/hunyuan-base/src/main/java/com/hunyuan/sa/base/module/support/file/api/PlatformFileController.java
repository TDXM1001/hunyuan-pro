package com.hunyuan.sa.base.module.support.file.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.hunyuan.sa.base.common.constant.RequestHeaderConst;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import com.hunyuan.sa.base.common.util.SmartResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 平台文件稳定 HTTP 接口。
 */
@RestController
@RequestMapping("/api/admin/v1/platform/files")
@Tag(name = "平台能力 - 文件")
public class PlatformFileController {

    @Resource
    private PlatformFileFacade platformFileFacade;

    @PostMapping
    @Operation(operationId = "platformFileUpload", summary = "上传文件")
    public ResponseDTO<PlatformFileUploadResult> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folder") Integer folder) {
        return platformFileFacade.upload(file, folder, SmartRequestUtil.getRequestUser());
    }

    @GetMapping("/url")
    @Operation(operationId = "platformFileResolveUrl", summary = "解析文件访问地址")
    public ResponseDTO<String> resolveUrl(@RequestParam("fileKey") String fileKey) {
        return platformFileFacade.resolveUrl(fileKey);
    }

    @PostMapping("/query")
    @Operation(operationId = "platformFileQuery", summary = "分页查询文件")
    @SaCheckPermission("support:file:query")
    public ResponseDTO<PageResult<PlatformFileSummary>> queryPage(
            @RequestBody @Valid PlatformFilePageQuery query) {
        return ResponseDTO.ok(platformFileFacade.queryPage(query));
    }

    @GetMapping("/download")
    @Operation(operationId = "platformFileDownload", summary = "下载文件")
    @SaCheckPermission("support:file:query")
    public void download(
            @RequestParam("fileKey") String fileKey,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String userAgent = JakartaServletUtil.getHeaderIgnoreCase(request, RequestHeaderConst.USER_AGENT);
        ResponseDTO<PlatformFileDownloadResult> download = platformFileFacade.download(fileKey, userAgent);
        if (!Boolean.TRUE.equals(download.getOk())) {
            SmartResponseUtil.write(response, download);
            return;
        }

        // 稳定接口只负责输出文件流，下载文件名和大小由 Facade 统一解析。
        PlatformFileDownloadResult result = download.getData();
        SmartResponseUtil.setDownloadFileHeader(response, result.fileName(), result.fileSize());
        response.getOutputStream().write(result.data());
    }
}
