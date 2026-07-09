package com.hunyuan.sa.base.module.support.file.service;

import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.module.support.file.constant.FileFolderTypeEnum;
import com.hunyuan.sa.base.module.support.file.dao.FileDao;
import com.hunyuan.sa.base.module.support.file.domain.entity.FileEntity;
import com.hunyuan.sa.base.module.support.file.domain.vo.FileUploadVO;
import com.hunyuan.sa.base.module.support.securityprotect.service.SecurityFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileServiceTest {

    private FileService fileService;

    private IFileStorageService fileStorageService;

    private FileDao fileDao;

    private SecurityFileService securityFileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService();
        fileStorageService = mock(IFileStorageService.class);
        fileDao = mock(FileDao.class);
        securityFileService = mock(SecurityFileService.class);

        ReflectionTestUtils.setField(fileService, "fileStorageService", fileStorageService);
        ReflectionTestUtils.setField(fileService, "fileDao", fileDao);
        ReflectionTestUtils.setField(fileService, "securityFileService", securityFileService);
    }

    @Test
    void fileUploadShouldPersistRecordAndKeepStorageWhenDbInsertSucceeds() {
        MultipartFile file = buildFile();
        when(securityFileService.checkFile(file)).thenReturn(ResponseDTO.ok());
        when(fileStorageService.upload(file, FileFolderTypeEnum.COMMON.getFolder()))
                .thenReturn(ResponseDTO.ok(buildUploadVO("private/common/demo.txt")));
        when(fileDao.insert(any(FileEntity.class))).thenAnswer(invocation -> {
            FileEntity entity = invocation.getArgument(0);
            entity.setFileId(99L);
            return 1;
        });

        ResponseDTO<FileUploadVO> response = fileService.fileUpload(file, FileFolderTypeEnum.COMMON.getValue(), buildRequestUser());

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getFileId()).isEqualTo(99L);
        ArgumentCaptor<FileEntity> entityCaptor = ArgumentCaptor.forClass(FileEntity.class);
        verify(fileDao).insert(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getFileKey()).isEqualTo("private/common/demo.txt");
        assertThat(entityCaptor.getValue().getFileName()).isEqualTo("demo.txt");
        verify(fileStorageService, never()).delete("private/common/demo.txt");
    }

    @Test
    void fileUploadShouldDeleteUploadedFileWhenDbInsertFails() {
        MultipartFile file = buildFile();
        RuntimeException dbException = new IllegalStateException("db down");
        when(securityFileService.checkFile(file)).thenReturn(ResponseDTO.ok());
        when(fileStorageService.upload(file, FileFolderTypeEnum.COMMON.getFolder()))
                .thenReturn(ResponseDTO.ok(buildUploadVO("private/common/demo.txt")));
        when(fileDao.insert(any(FileEntity.class))).thenThrow(dbException);
        when(fileStorageService.delete("private/common/demo.txt")).thenReturn(ResponseDTO.ok());

        assertThatThrownBy(() -> fileService.fileUpload(file, FileFolderTypeEnum.COMMON.getValue(), buildRequestUser()))
                .isSameAs(dbException);

        verify(fileStorageService).delete("private/common/demo.txt");
    }

    @Test
    void fileUploadShouldKeepOriginalDbExceptionWhenCompensationDeleteFails() {
        MultipartFile file = buildFile();
        RuntimeException dbException = new IllegalStateException("db down");
        when(securityFileService.checkFile(file)).thenReturn(ResponseDTO.ok());
        when(fileStorageService.upload(file, FileFolderTypeEnum.COMMON.getFolder()))
                .thenReturn(ResponseDTO.ok(buildUploadVO("private/common/demo.txt")));
        when(fileDao.insert(any(FileEntity.class))).thenThrow(dbException);
        when(fileStorageService.delete("private/common/demo.txt"))
                .thenThrow(new IllegalStateException("delete failed"));

        assertThatThrownBy(() -> fileService.fileUpload(file, FileFolderTypeEnum.COMMON.getValue(), buildRequestUser()))
                .isSameAs(dbException);

        verify(fileStorageService).delete("private/common/demo.txt");
    }

    @Test
    void fileUploadShouldReturnStorageFailureWithoutDbInsertOrCompensation() {
        MultipartFile file = buildFile();
        when(securityFileService.checkFile(file)).thenReturn(ResponseDTO.ok());
        when(fileStorageService.upload(file, FileFolderTypeEnum.COMMON.getFolder()))
                .thenReturn(ResponseDTO.userErrorParam("上传失败"));

        ResponseDTO<FileUploadVO> response = fileService.fileUpload(file, FileFolderTypeEnum.COMMON.getValue(), buildRequestUser());

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).contains("上传失败");
        verify(fileDao, never()).insert(any(FileEntity.class));
        verify(fileStorageService, never()).delete(any());
    }

    private MockMultipartFile buildFile() {
        return new MockMultipartFile(
                "file",
                "demo.txt",
                "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8)
        );
    }

    private FileUploadVO buildUploadVO(String fileKey) {
        FileUploadVO uploadVO = new FileUploadVO();
        uploadVO.setFileKey(fileKey);
        uploadVO.setFileName("demo.txt");
        uploadVO.setFileType("txt");
        uploadVO.setFileSize(5L);
        uploadVO.setFileUrl("http://localhost/upload/" + fileKey);
        return uploadVO;
    }

    private RequestUser buildRequestUser() {
        return new RequestUser() {
            @Override
            public Long getUserId() {
                return 1L;
            }

            @Override
            public String getUserName() {
                return "管理员";
            }

            @Override
            public UserTypeEnum getUserType() {
                return UserTypeEnum.ADMIN_EMPLOYEE;
            }

            @Override
            public String getIp() {
                return "127.0.0.1";
            }

            @Override
            public String getUserAgent() {
                return "JUnit";
            }
        };
    }
}
