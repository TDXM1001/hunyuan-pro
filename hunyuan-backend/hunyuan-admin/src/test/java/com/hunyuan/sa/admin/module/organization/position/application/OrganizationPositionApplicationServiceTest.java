package com.hunyuan.sa.admin.module.organization.position.application;

import com.hunyuan.sa.admin.module.organization.OrganizationModuleAvailability;
import com.hunyuan.sa.admin.module.organization.position.domain.Position;
import com.hunyuan.sa.admin.module.organization.position.domain.PositionCommand;
import com.hunyuan.sa.admin.module.organization.position.domain.PositionEmployeeReferencePort;
import com.hunyuan.sa.admin.module.organization.position.domain.PositionRepository;
import com.hunyuan.sa.base.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("岗位目录应用服务")
class OrganizationPositionApplicationServiceTest {

    @Mock
    private PositionRepository positionRepository;
    @Mock
    private PositionEmployeeReferencePort employeeReferencePort;
    @Mock
    private OrganizationModuleAvailability moduleAvailability;

    private OrganizationPositionApplicationService service;

    @BeforeEach
    void 初始化服务() {
        service = new OrganizationPositionApplicationService();
        ReflectionTestUtils.setField(service, "positionRepository", positionRepository);
        ReflectionTestUtils.setField(service, "employeeReferencePort", employeeReferencePort);
        ReflectionTestUtils.setField(service, "moduleAvailability", moduleAvailability);
    }

    @Test
    @DisplayName("创建岗位时清理文本并校验重名")
    void 创建岗位时清理文本并校验重名() {
        when(positionRepository.existsByName("研发工程师", null)).thenReturn(false);
        when(positionRepository.insert(org.mockito.ArgumentMatchers.any(Position.class))).thenReturn(20L);

        service.create(new PositionCommand(" 研发工程师 ", " P5 ", 10, " 核心岗位 "));

        ArgumentCaptor<Position> captor = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).insert(captor.capture());
        assertThat(captor.getValue())
                .extracting(Position::positionName, Position::positionLevel, Position::sort, Position::remark)
                .containsExactly("研发工程师", "P5", 10, "核心岗位");
    }

    @Test
    @DisplayName("创建重名岗位时拒绝写入")
    void 创建重名岗位时拒绝写入() {
        when(positionRepository.existsByName("研发工程师", null)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new PositionCommand("研发工程师", null, 10, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("岗位名称已存在");

        verify(positionRepository, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("删除不存在岗位时返回岗位不存在")
    void 删除不存在岗位时返回岗位不存在() {
        when(positionRepository.exists(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("岗位不存在");

        verify(positionRepository, never()).delete(999L);
    }

    @Test
    @DisplayName("岗位被员工引用时拒绝删除并返回引用人数")
    void 岗位被员工引用时拒绝删除并返回引用人数() {
        when(positionRepository.exists(20L)).thenReturn(true);
        when(employeeReferencePort.countNonDeletedEmployees(20L)).thenReturn(3);

        assertThatThrownBy(() -> service.delete(20L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("3 名员工");

        verify(positionRepository, never()).delete(20L);
    }

    @Test
    @DisplayName("未被员工引用的岗位允许删除")
    void 未被员工引用的岗位允许删除() {
        when(positionRepository.exists(20L)).thenReturn(true);
        when(employeeReferencePort.countNonDeletedEmployees(20L)).thenReturn(0);

        service.delete(20L);

        verify(positionRepository).delete(20L);
    }

    @Test
    @DisplayName("批量删除先完成全部引用检查再执行删除")
    void 批量删除先完成全部引用检查再执行删除() {
        when(positionRepository.exists(20L)).thenReturn(true);
        when(positionRepository.exists(30L)).thenReturn(true);
        when(employeeReferencePort.countNonDeletedEmployees(20L)).thenReturn(0);
        when(employeeReferencePort.countNonDeletedEmployees(30L)).thenReturn(2);

        assertThatThrownBy(() -> service.deleteBatch(List.of(20L, 30L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2 名员工");

        verify(positionRepository, never()).delete(20L);
        verify(positionRepository, never()).delete(30L);
    }
}
