package com.hunyuan.sa.bpm.candidate;

import com.hunyuan.sa.bpm.api.identity.BpmIdentityOptionSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.candidate.domain.vo.BpmIdentityOptionPageVO;
import com.hunyuan.sa.bpm.module.candidate.service.BpmPolicyIdentityOptionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BpmPolicyIdentityOptionServiceTest {

    @Test
    void queryShouldReturnNamedOptionsWithoutExposingInternalObjects() {
        BpmOrgIdentityGateway gateway = mock(BpmOrgIdentityGateway.class);
        when(gateway.queryIdentityOptions("ROLE", "财务", null)).thenReturn(List.of(
                new BpmIdentityOptionSnapshot("ROLE", 8L, "财务经理", null, null, false)
        ));
        BpmPolicyIdentityOptionService service = new BpmPolicyIdentityOptionService(gateway);

        BpmIdentityOptionPageVO result = service.query("ROLE", "财务", null, 1, 20);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).singleElement().satisfies(option -> {
            assertThat(option.stableId()).isEqualTo(8L);
            assertThat(option.displayName()).isEqualTo("财务经理");
            assertThat(option.kind()).isEqualTo("ROLE");
        });
    }
}
