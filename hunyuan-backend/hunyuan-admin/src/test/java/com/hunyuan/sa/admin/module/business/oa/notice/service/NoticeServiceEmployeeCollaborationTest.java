package com.hunyuan.sa.admin.module.business.oa.notice.service;

import com.hunyuan.sa.admin.module.business.oa.notice.constant.NoticeVisibleRangeDataTypeEnum;
import com.hunyuan.sa.admin.module.business.oa.notice.dao.NoticeDao;
import com.hunyuan.sa.admin.module.business.oa.notice.domain.entity.NoticeEntity;
import com.hunyuan.sa.admin.module.business.oa.notice.domain.form.NoticeAddForm;
import com.hunyuan.sa.admin.module.business.oa.notice.domain.form.NoticeVisibleRangeForm;
import com.hunyuan.sa.admin.module.business.oa.notice.domain.vo.NoticeTypeVO;
import com.hunyuan.sa.admin.module.business.oa.notice.domain.vo.NoticeUpdateFormVO;
import com.hunyuan.sa.admin.module.business.oa.notice.domain.vo.NoticeVisibleRangeVO;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeCollaborationProfile;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeDirectoryFacade;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceEmployeeCollaborationTest {

    @Mock
    private NoticeDao noticeDao;
    @Mock
    private NoticeTypeService noticeTypeService;
    @Mock
    private EmployeeDirectoryFacade employeeDirectoryFacade;

    private NoticeService service;

    @BeforeEach
    void setUp() {
        service = new NoticeService();
        ReflectionTestUtils.setField(service, "noticeDao", noticeDao);
        ReflectionTestUtils.setField(service, "noticeTypeService", noticeTypeService);
        ReflectionTestUtils.setField(service, "employeeDirectoryFacade", employeeDirectoryFacade);
    }

    @Test
    void visibleRangeValidationKeepsPersistedDeletedEmployeeCompatibility() {
        NoticeTypeVO noticeType = new NoticeTypeVO();
        noticeType.setNoticeTypeId(3L);
        when(noticeTypeService.getByNoticeTypeId(3L)).thenReturn(noticeType);
        when(employeeDirectoryFacade.findCollaborationProfilesByIds(List.of(7L)))
                .thenReturn(List.of(new EmployeeCollaborationProfile(
                        7L, "Archived employee", 20L, false, true, true)));

        NoticeAddForm form = new NoticeAddForm();
        form.setNoticeTypeId(3L);
        form.setAllVisibleFlag(false);
        form.setVisibleRangeList(List.of(new NoticeVisibleRangeForm(
                NoticeVisibleRangeDataTypeEnum.EMPLOYEE.getValue(), 7L)));

        ResponseDTO<String> response = ReflectionTestUtils.invokeMethod(
                service, "checkAndBuildVisibleRange", form);

        assertThat(response).isNotNull();
        assertThat(response.getOk()).isTrue();
    }

    @Test
    void updateFormResolvesEmployeeNamesWithoutLegacyEntities() {
        NoticeEntity notice = new NoticeEntity();
        notice.setNoticeId(9L);
        notice.setNoticeTypeId(3L);
        notice.setAllVisibleFlag(false);
        when(noticeDao.selectById(9L)).thenReturn(notice);

        NoticeTypeVO noticeType = new NoticeTypeVO();
        noticeType.setNoticeTypeName("Internal");
        when(noticeTypeService.getByNoticeTypeId(3L)).thenReturn(noticeType);

        NoticeVisibleRangeVO employeeRange = new NoticeVisibleRangeVO();
        employeeRange.setDataType(NoticeVisibleRangeDataTypeEnum.EMPLOYEE.getValue());
        employeeRange.setDataId(7L);
        when(noticeDao.queryVisibleRange(9L)).thenReturn(List.of(employeeRange));
        when(employeeDirectoryFacade.findCollaborationProfilesByIds(List.of(7L)))
                .thenReturn(List.of(new EmployeeCollaborationProfile(
                        7L, "Alice", 20L, false, false, false)));

        NoticeUpdateFormVO result = service.getUpdateFormVO(9L);

        assertThat(result.getVisibleRangeList()).singleElement()
                .extracting(NoticeVisibleRangeVO::getDataName)
                .isEqualTo("Alice");
    }
}
