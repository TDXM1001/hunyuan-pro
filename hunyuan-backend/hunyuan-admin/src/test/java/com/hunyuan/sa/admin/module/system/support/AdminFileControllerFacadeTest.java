package com.hunyuan.sa.admin.module.system.support;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.file.api.PlatformFileFacade;
import com.hunyuan.sa.base.module.support.file.api.PlatformFilePageQuery;
import com.hunyuan.sa.base.module.support.file.api.PlatformFileSummary;
import com.hunyuan.sa.base.module.support.file.domain.form.FileQueryForm;
import com.hunyuan.sa.base.module.support.file.domain.vo.FileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定历史文件管理查询通过稳定 Facade 适配，不重新直连历史文件服务。
 */
@ExtendWith(MockitoExtension.class)
class AdminFileControllerFacadeTest {

    @Mock
    private PlatformFileFacade platformFileFacade;

    private AdminFileController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminFileController();
        ReflectionTestUtils.setField(controller, "platformFileFacade", platformFileFacade);
    }

    @Test
    void mapsLegacyQueryAndStableSummaryWithoutChangingResponseShape() {
        PlatformFileSummary summary = new PlatformFileSummary();
        summary.setFileId(18L);
        summary.setFolderType(1);
        summary.setFileName("验收文件.png");
        summary.setFileSize(128);
        summary.setFileType("png");
        summary.setFileKey("private/common/acceptance.png");
        summary.setCreatorName("hunyuan");

        PageResult<PlatformFileSummary> stablePage = new PageResult<>();
        stablePage.setPageNum(1L);
        stablePage.setPageSize(10L);
        stablePage.setTotal(1L);
        stablePage.setPages(1L);
        stablePage.setEmptyFlag(false);
        stablePage.setList(List.of(summary));

        FileQueryForm queryForm = new FileQueryForm();
        queryForm.setPageNum(1L);
        queryForm.setPageSize(10L);
        queryForm.setFolderType(1);
        queryForm.setFileName("验收文件");
        queryForm.setCreateTimeBegin(LocalDate.of(2026, 7, 1));
        when(platformFileFacade.queryPage(argThat(query ->
                query.getPageNum().equals(1L)
                        && query.getPageSize().equals(10L)
                        && query.getFolderType().equals(1)
                        && query.getFileName().equals("验收文件")
                        && query.getCreateTimeBegin().equals(LocalDate.of(2026, 7, 1)))))
                .thenReturn(stablePage);

        ResponseDTO<PageResult<FileVO>> response = controller.queryPage(queryForm);

        assertThat(response.getOk()).isTrue();
        assertThat(response.getData().getTotal()).isEqualTo(1L);
        assertThat(response.getData().getList()).singleElement().satisfies(file -> {
            assertThat(file.getFileId()).isEqualTo(18L);
            assertThat(file.getFileName()).isEqualTo("验收文件.png");
            assertThat(file.getFileKey()).isEqualTo("private/common/acceptance.png");
            assertThat(file.getCreatorName()).isEqualTo("hunyuan");
        });
        verify(platformFileFacade).queryPage(argThat(query -> query instanceof PlatformFilePageQuery));
    }
}
