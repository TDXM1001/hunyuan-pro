package com.hunyuan.sa.bpm.integration;

import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorEndpointPolicy;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BpmConnectorEndpointPolicyTest {

    private final BpmConnectorEndpointPolicy policy = new BpmConnectorEndpointPolicy();

    @Test
    void shouldRejectPlainHttpAndPrivateAddresses() throws Exception {
        assertThatThrownBy(() -> policy.validate(
                URI.create("http://api.example.com/expense"),
                List.of(InetAddress.getByName("93.184.216.34"))
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("HTTPS");

        assertThatThrownBy(() -> policy.validate(
                URI.create("https://internal.example.com/expense"),
                List.of(InetAddress.getByName("127.0.0.1"), InetAddress.getByName("10.0.0.8"))
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("受保护网络");
    }

    @Test
    void shouldAllowHttpsEndpointResolvedOnlyToPublicAddresses() throws Exception {
        assertThatCode(() -> policy.validate(
                URI.create("https://api.example.com/expense"),
                List.of(InetAddress.getByName("93.184.216.34"))
        )).doesNotThrowAnyException();
    }
}
