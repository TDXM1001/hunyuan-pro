package com.hunyuan.sa.bpm.integration;

import com.hunyuan.sa.bpm.module.integration.domain.model.BpmProcessBindingMatch;
import com.hunyuan.sa.bpm.module.integration.domain.model.BpmSourceApplicationPrincipal;
import com.hunyuan.sa.bpm.module.integration.service.BpmExternalEmployeeMappingService;
import com.hunyuan.sa.bpm.module.integration.service.BpmProcessBindingService;
import com.hunyuan.sa.bpm.module.integration.service.BpmSourceSystemService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.List;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmProcessBindingVersionEntity;
import com.hunyuan.sa.bpm.module.integration.dao.BpmProcessBindingVersionDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmSourceApplicationEntity;
import com.hunyuan.sa.bpm.module.integration.dao.BpmSourceApplicationDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BpmM6ControlPlaneContractTest {

    @Test
    void publishedBindingMustResolveExactlyOneFrozenDefinition() {
        BpmProcessBindingService service = new BpmProcessBindingService(
                Mockito.mock(com.hunyuan.sa.bpm.module.integration.dao.BpmProcessBindingVersionDao.class)
        );

        assertThatThrownBy(() -> service.resolve("purchase", 10L, "DEFAULT", Map.of("amount", 5000)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("流程绑定");

        BpmProcessBindingMatch match = new BpmProcessBindingMatch(9L, 101L, "purchase-main", 3);
        assertThat(match.graphDefinitionVersionId()).isEqualTo(101L);
        assertThat(match.bindingVersion()).isEqualTo(3);
    }

    @Test
    void bindingMustEvaluateTypedFactsBeforePriority() {
        BpmProcessBindingVersionDao dao=Mockito.mock(BpmProcessBindingVersionDao.class);
        BpmProcessBindingVersionEntity low=binding(1L,10,"{\"all\":[{\"fact\":\"amount\",\"operator\":\"LT\",\"value\":5000}]}");
        BpmProcessBindingVersionEntity high=binding(2L,20,"{\"all\":[{\"fact\":\"amount\",\"operator\":\"GTE\",\"value\":5000}]}");
        Mockito.when(dao.selectList(Mockito.any())).thenReturn(List.of(high,low));
        BpmProcessBindingMatch match=new BpmProcessBindingService(dao).resolve("purchase",10L,"DEFAULT",Map.of("amount",6000));
        assertThat(match.bindingVersionId()).isEqualTo(2L);
    }

    private BpmProcessBindingVersionEntity binding(Long id,int priority,String condition){BpmProcessBindingVersionEntity e=new BpmProcessBindingVersionEntity();e.setBindingVersionId(id);e.setBindingKey("binding-"+id);e.setBindingVersion(1);e.setGraphDefinitionVersionId(100L+id);e.setPriority(priority);e.setConditionJson(condition);return e;}

    @Test
    void externalApplicationCannotImpersonateAnUnmappedEmployee() {
        BpmExternalEmployeeMappingService service = new BpmExternalEmployeeMappingService(
                Mockito.mock(com.hunyuan.sa.bpm.module.integration.dao.BpmExternalEmployeeMappingDao.class),
                Clock.fixed(Instant.parse("2026-07-13T10:00:00Z"), ZoneOffset.UTC)
        );
        BpmSourceApplicationPrincipal principal = new BpmSourceApplicationPrincipal(
                1L, "PURCHASE", "purchase-app", "process:start task:read task:action", "ACTIVE"
        );

        assertThatThrownBy(() -> service.requireEmployee(principal, "buyer-100"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("员工映射");
    }

    @Test
    void sourceApplicationSignatureMustCoverTimestampNonceAndBody() {
        BpmSourceSystemService service = new BpmSourceSystemService(
                Mockito.mock(com.hunyuan.sa.bpm.module.integration.dao.BpmSourceSystemVersionDao.class),
                Mockito.mock(com.hunyuan.sa.bpm.module.integration.dao.BpmSourceApplicationDao.class),
                Mockito.mock(com.hunyuan.sa.bpm.module.integration.dao.BpmExternalRequestNonceDao.class),
                Clock.fixed(Instant.parse("2026-07-13T10:00:00Z"), ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> service.authenticate(
                "purchase-app", "1783936800", "nonce-1", "bad-signature", "POST", "/open/bpm/v1/processes", "{\"requestId\":\"r1\"}"
        )).isInstanceOf(SecurityException.class);
    }

    @Test
    void expiredApplicationMustBeRejectedBeforeSignatureVerification() {
        BpmSourceApplicationDao appDao=Mockito.mock(BpmSourceApplicationDao.class);
        BpmSourceApplicationEntity app=new BpmSourceApplicationEntity();app.setApplicationId(1L);app.setApplicationCode("purchase-app");app.setStatus("ACTIVE");app.setExpiresAt(java.time.LocalDateTime.of(2026,7,13,9,0));
        Mockito.when(appDao.selectOne(Mockito.any())).thenReturn(app);
        BpmSourceSystemService service=new BpmSourceSystemService(Mockito.mock(com.hunyuan.sa.bpm.module.integration.dao.BpmSourceSystemVersionDao.class),appDao,Mockito.mock(com.hunyuan.sa.bpm.module.integration.dao.BpmExternalRequestNonceDao.class),Clock.fixed(Instant.parse("2026-07-13T10:00:00Z"),ZoneOffset.UTC));
        assertThatThrownBy(()->service.authenticate("purchase-app","1783936800","nonce-1","signature","POST","/open/bpm/v1/processes","{}"))
                .isInstanceOf(SecurityException.class).hasMessageContaining("过期");
    }
}
