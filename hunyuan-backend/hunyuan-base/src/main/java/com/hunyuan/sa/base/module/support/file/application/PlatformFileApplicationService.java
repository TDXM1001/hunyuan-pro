package com.hunyuan.sa.base.module.support.file.application;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.module.support.file.api.PlatformFileDownloadResult;
import com.hunyuan.sa.base.module.support.file.api.PlatformFileFacade;
import com.hunyuan.sa.base.module.support.file.api.PlatformFilePageQuery;
import com.hunyuan.sa.base.module.support.file.api.PlatformFileSummary;
import com.hunyuan.sa.base.module.support.file.api.PlatformFileUploadResult;
import com.hunyuan.sa.base.module.support.file.domain.form.FileQueryForm;
import com.hunyuan.sa.base.module.support.file.domain.vo.FileDownloadVO;
import com.hunyuan.sa.base.module.support.file.domain.vo.FileMetadataVO;
import com.hunyuan.sa.base.module.support.file.domain.vo.FileUploadVO;
import com.hunyuan.sa.base.module.support.file.domain.vo.FileVO;
import com.hunyuan.sa.base.module.support.file.service.FileService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 平台文件公开用例实现，负责把历史文件对象转换为稳定契约。
 */
@Service
public class PlatformFileApplicationService implements PlatformFileFacade {

    @Resource
    private FileService fileService;

    @Override
    public ResponseDTO<PlatformFileUploadResult> upload(
            MultipartFile file, Integer folderType, RequestUser requestUser) {
        ResponseDTO<FileUploadVO> response = fileService.fileUpload(file, folderType, requestUser);
        if (!Boolean.TRUE.equals(response.getOk())) {
            return ResponseDTO.error(response);
        }
        FileUploadVO upload = response.getData();
        return ResponseDTO.ok(new PlatformFileUploadResult(
                upload.getFileId(),
                upload.getFileName(),
                upload.getFileUrl(),
                upload.getFileKey(),
                upload.getFileSize(),
                upload.getFileType()));
    }

    @Override
    public ResponseDTO<String> resolveUrl(String fileKey) {
        return fileService.getFileUrl(fileKey);
    }

    @Override
    public PageResult<PlatformFileSummary> queryPage(PlatformFilePageQuery query) {
        FileQueryForm legacyQuery = SmartBeanUtil.copy(query, FileQueryForm.class);
        PageResult<FileVO> legacyResult = fileService.queryPage(legacyQuery);

        PageResult<PlatformFileSummary> result = new PageResult<>();
        result.setPageNum(legacyResult.getPageNum());
        result.setPageSize(legacyResult.getPageSize());
        result.setTotal(legacyResult.getTotal());
        result.setPages(legacyResult.getPages());
        result.setEmptyFlag(legacyResult.getEmptyFlag());
        result.setList(SmartBeanUtil.copyList(legacyResult.getList(), PlatformFileSummary.class));
        return result;
    }

    @Override
    public ResponseDTO<PlatformFileDownloadResult> download(String fileKey, String userAgent) {
        ResponseDTO<FileDownloadVO> response = fileService.getDownloadFile(fileKey, userAgent);
        if (!Boolean.TRUE.equals(response.getOk())) {
            return ResponseDTO.error(response);
        }

        FileDownloadVO download = response.getData();
        FileMetadataVO metadata = download.getMetadata();
        return ResponseDTO.ok(new PlatformFileDownloadResult(
                download.getData(), metadata.getFileName(), metadata.getFileSize()));
    }
}
