package com.hunyuan.sa.base.module.support.file.controller;

import cn.hutool.extra.servlet.JakartaServletUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.hunyuan.sa.base.common.constant.RequestHeaderConst;
import com.hunyuan.sa.base.common.controller.SupportBaseController;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import com.hunyuan.sa.base.common.util.SmartResponseUtil;
import com.hunyuan.sa.base.constant.SwaggerTagConst;
import com.hunyuan.sa.base.module.support.file.api.PlatformFileDownloadResult;
import com.hunyuan.sa.base.module.support.file.api.PlatformFileFacade;
import com.hunyuan.sa.base.module.support.file.api.PlatformFileUploadResult;
import com.hunyuan.sa.base.module.support.file.domain.vo.FileUploadVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件服务
 *
 * @Author 1024创新实验室: 罗伊
 * @Date 2019年10月11日 15:34:47
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RestController
@Tag(name = SwaggerTagConst.Support.FILE)
public class FileController extends SupportBaseController {

    @Resource
    private PlatformFileFacade platformFileFacade;


    @Operation(summary = "文件上传 @author 胡克")
    @PostMapping("/file/upload")
    public ResponseDTO<FileUploadVO> upload(@RequestParam MultipartFile file, @RequestParam Integer folder) {
        RequestUser requestUser = SmartRequestUtil.getRequestUser();
        ResponseDTO<PlatformFileUploadResult> response = platformFileFacade.upload(file, folder, requestUser);
        if (!Boolean.TRUE.equals(response.getOk())) {
            return ResponseDTO.error(response);
        }
        PlatformFileUploadResult upload = response.getData();
        FileUploadVO legacyResult = new FileUploadVO();
        legacyResult.setFileId(upload.fileId());
        legacyResult.setFileName(upload.fileName());
        legacyResult.setFileUrl(upload.fileUrl());
        legacyResult.setFileKey(upload.fileKey());
        legacyResult.setFileSize(upload.fileSize());
        legacyResult.setFileType(upload.fileType());
        return new ResponseDTO<>(response.getCode(), response.getLevel(), true, response.getMsg(), legacyResult);
    }

    @Operation(summary = "获取文件URL：根据fileKey @author 胡克")
    @GetMapping("/file/getFileUrl")
    public ResponseDTO<String> getUrl(@RequestParam String fileKey) {
        return platformFileFacade.resolveUrl(fileKey);
    }

    @Operation(summary = "下载文件流（根据fileKey） @author 胡克")
    @GetMapping("/file/downLoad")
    public void downLoad(@RequestParam String fileKey, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userAgent = JakartaServletUtil.getHeaderIgnoreCase(request, RequestHeaderConst.USER_AGENT);
        ResponseDTO<PlatformFileDownloadResult> downloadFileResult = platformFileFacade.download(fileKey, userAgent);
        if (!downloadFileResult.getOk()) {
            SmartResponseUtil.write(response, downloadFileResult);
            return;
        }
        // 下载文件信息
        PlatformFileDownloadResult fileDownloadVO = downloadFileResult.getData();
        // 设置下载消息头
        SmartResponseUtil.setDownloadFileHeader(response, fileDownloadVO.fileName(), fileDownloadVO.fileSize());
        // 下载
        response.getOutputStream().write(fileDownloadVO.data());
    }
}
