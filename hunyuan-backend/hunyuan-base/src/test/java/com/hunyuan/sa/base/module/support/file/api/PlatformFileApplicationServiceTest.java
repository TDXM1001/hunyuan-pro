package com.hunyuan.sa.base.module.support.file.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.file.application.PlatformFileApplicationService;
import com.hunyuan.sa.base.module.support.file.domain.form.FileQueryForm;
import com.hunyuan.sa.base.module.support.file.domain.vo.FileDownloadVO;
import com.hunyuan.sa.base.module.support.file.domain.vo.FileMetadataVO;
import com.hunyuan.sa.base.module.support.file.domain.vo.FileUploadVO;
import com.hunyuan.sa.base.module.support.file.domain.vo.FileVO;
import com.hunyuan.sa.base.module.support.file.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定稳定文件 Facade 与历史服务之间的对象映射和失败语义。
 */
@ExtendWith(MockitoExtension.class)
class PlatformFileApplicationServiceTest {

    @Mock
    private FileService fileService;

    private PlatformFileApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PlatformFileApplicationService();
        ReflectionTestUtils.setField(service, "fileService", fileService);
    }

    @Test
    void mapsLegacyUploadResultToStableContract() {
        MockMultipartFile file = new MockMultipartFile("file", "头像.png", "image/png", new byte[]{1});
        FileUploadVO legacy = new FileUploadVO();
        legacy.setFileId(8L);
        legacy.setFileName("头像.png");
        legacy.setFileUrl("http://localhost/upload/private/common/avatar.png");
        legacy.setFileKey("private/common/avatar.png");
        legacy.setFileSize(1L);
        legacy.setFileType("png");
        RequestUser requestUser = org.mockito.Mockito.mock(RequestUser.class);
        when(fileService.fileUpload(file, 1, requestUser)).thenReturn(ResponseDTO.ok(legacy));

        ResponseDTO<PlatformFileUploadResult> response = service.upload(file, 1, requestUser);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData()).isEqualTo(new PlatformFileUploadResult(
                8L,
                "头像.png",
                "http://localhost/upload/private/common/avatar.png",
                "private/common/avatar.png",
                1L,
                "png"));
    }

    @Test
    void keepsLegacyFailureCodeAndMessage() {
        MockMultipartFile file = new MockMultipartFile("file", "头像.png", "image/png", new byte[]{1});
        RequestUser requestUser = org.mockito.Mockito.mock(RequestUser.class);
        when(fileService.fileUpload(file, 1, requestUser)).thenReturn(ResponseDTO.userErrorParam("文件类型不允许"));

        ResponseDTO<PlatformFileUploadResult> response = service.upload(file, 1, requestUser);

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).isEqualTo("文件类型不允许");
    }

    @Test
    void mapsLegacyPageToStableContract() {
        FileVO legacyFile = new FileVO();
        legacyFile.setFileId(12L);
        legacyFile.setFolderType(1);
        legacyFile.setFileName("设计图.png");
        legacyFile.setFileSize(2048);
        legacyFile.setFileType("png");
        legacyFile.setFileKey("private/common/design.png");
        legacyFile.setCreatorId(7L);
        legacyFile.setCreatorName("hunyuan");
        legacyFile.setCreatorUserType(1);
        legacyFile.setFileUrl("http://localhost/upload/private/common/design.png");
        legacyFile.setCreateTime(LocalDateTime.of(2026, 7, 24, 10, 0));

        PageResult<FileVO> legacyPage = new PageResult<>();
        legacyPage.setPageNum(2L);
        legacyPage.setPageSize(20L);
        legacyPage.setTotal(21L);
        legacyPage.setPages(2L);
        legacyPage.setEmptyFlag(false);
        legacyPage.setList(List.of(legacyFile));
        when(fileService.queryPage(any(FileQueryForm.class))).thenReturn(legacyPage);

        PlatformFilePageQuery query = new PlatformFilePageQuery();
        query.setPageNum(2L);
        query.setPageSize(20L);
        query.setFolderType(1);
        query.setFileName("设计图");
        query.setCreateTimeBegin(LocalDate.of(2026, 7, 1));
        query.setCreateTimeEnd(LocalDate.of(2026, 7, 24));

        PageResult<PlatformFileSummary> result = service.queryPage(query);

        assertThat(result.getTotal()).isEqualTo(21L);
        assertThat(result.getList()).hasSize(1);
        PlatformFileSummary file = result.getList().get(0);
        assertThat(file.getFileId()).isEqualTo(12L);
        assertThat(file.getFileName()).isEqualTo("设计图.png");
        assertThat(file.getCreatorName()).isEqualTo("hunyuan");
        assertThat(file.getFileUrl()).isEqualTo("http://localhost/upload/private/common/design.png");
        verify(fileService).queryPage(argThat(legacyQuery ->
                legacyQuery.getPageNum().equals(2L)
                        && legacyQuery.getPageSize().equals(20L)
                        && legacyQuery.getFolderType().equals(1)
                        && legacyQuery.getFileName().equals("设计图")
                        && legacyQuery.getCreateTimeBegin().equals(LocalDate.of(2026, 7, 1))
                        && legacyQuery.getCreateTimeEnd().equals(LocalDate.of(2026, 7, 24))));
    }

    @Test
    void mapsLegacyDownloadToStableContract() {
        FileMetadataVO metadata = new FileMetadataVO();
        metadata.setFileName("设计图.png");
        metadata.setFileSize(3L);
        FileDownloadVO legacy = new FileDownloadVO();
        legacy.setData(new byte[]{1, 2, 3});
        legacy.setMetadata(metadata);
        when(fileService.getDownloadFile("private/common/design.png", "浏览器"))
                .thenReturn(ResponseDTO.ok(legacy));

        ResponseDTO<PlatformFileDownloadResult> response = service.download(
                "private/common/design.png", "浏览器");

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().fileName()).isEqualTo("设计图.png");
        assertThat(response.getData().fileSize()).isEqualTo(3L);
        assertThat(response.getData().data()).containsExactly(1, 2, 3);
    }

    @Test
    void keepsLegacyDownloadFailureCodeAndMessage() {
        when(fileService.getDownloadFile("missing.png", "浏览器"))
                .thenReturn(ResponseDTO.userErrorParam("文件不存在"));

        ResponseDTO<PlatformFileDownloadResult> response = service.download("missing.png", "浏览器");

        assertThat(response.getOk()).isFalse();
        assertThat(response.getMsg()).isEqualTo("文件不存在");
    }
}
