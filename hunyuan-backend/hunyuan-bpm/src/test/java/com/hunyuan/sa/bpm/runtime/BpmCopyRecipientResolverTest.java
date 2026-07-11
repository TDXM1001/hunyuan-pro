package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.definition.domain.entity.BpmDefinitionNodeEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmInstanceEntity;
import com.hunyuan.sa.bpm.module.runtime.service.BpmCopyRecipientResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class BpmCopyRecipientResolverTest {

    private BpmOrgIdentityGateway identityGateway;
    private BpmCopyRecipientResolver resolver;

    @BeforeEach
    void setUp() {
        identityGateway = Mockito.mock(BpmOrgIdentityGateway.class);
        resolver = new BpmCopyRecipientResolver();
        setField(resolver, "bpmOrgIdentityGateway", identityGateway);
    }

    @Test
    void resolveShouldReturnDeduplicatedFixedEmployees() {
        when(identityGateway.requireEmployee(11L)).thenReturn(employee(11L));
        when(identityGateway.requireEmployee(12L)).thenReturn(employee(12L));

        List<Long> result = resolver.resolve(
                instance(),
                node("{\"candidateResolverType\":\"EMPLOYEE\",\"employeeIds\":[11,12,11]}")
        );

        assertThat(result).containsExactly(11L, 12L);
    }

    @Test
    void resolveShouldKeepOnlyAvailableRoleMembers() {
        when(identityGateway.listEmployeeIdsByRoleId(7L)).thenReturn(List.of(21L, 22L));
        when(identityGateway.requireEmployee(21L)).thenThrow(new IllegalArgumentException("员工已停用"));
        when(identityGateway.requireEmployee(22L)).thenReturn(employee(22L));

        List<Long> result = resolver.resolve(
                instance(),
                node("{\"candidateResolverType\":\"ROLE\",\"roleId\":7}")
        );

        assertThat(result).containsExactly(22L);
    }

    @Test
    void resolveShouldUseStartEmployeeSnapshot() {
        when(identityGateway.requireEmployee(31L)).thenReturn(employee(31L));
        BpmInstanceEntity instance = instance();
        instance.setStartEmployeeId(31L);

        List<Long> result = resolver.resolve(
                instance,
                node("{\"candidateResolverType\":\"START_EMPLOYEE\"}")
        );

        assertThat(result).containsExactly(31L);
    }

    private BpmInstanceEntity instance() {
        BpmInstanceEntity instance = new BpmInstanceEntity();
        instance.setInstanceId(81L);
        instance.setDefinitionId(18L);
        instance.setStartDepartmentIdSnapshot(5L);
        instance.setCurrentFormDataSnapshotJson("{}");
        return instance;
    }

    private BpmDefinitionNodeEntity node(String snapshotJson) {
        BpmDefinitionNodeEntity node = new BpmDefinitionNodeEntity();
        node.setNodeKey("notify_finance");
        node.setNodeNameSnapshot("抄送财务");
        node.setCompiledNodeSnapshotJson(snapshotJson);
        return node;
    }

    private BpmEmployeeSnapshot employee(Long employeeId) {
        return new BpmEmployeeSnapshot(employeeId, "员工" + employeeId, 5L, "财务部", null, null);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("设置测试字段失败: " + fieldName, ex);
        }
    }
}
