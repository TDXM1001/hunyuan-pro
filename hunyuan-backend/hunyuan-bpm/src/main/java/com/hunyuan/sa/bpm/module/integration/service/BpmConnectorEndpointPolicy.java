package com.hunyuan.sa.bpm.module.integration.service;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * 登记连接器端点的 SSRF 安全策略。
 */
@Component
public class BpmConnectorEndpointPolicy {

    public void validate(URI endpoint) {
        if (endpoint == null || endpoint.getHost() == null) {
            throw new IllegalArgumentException("连接器端点必须包含有效主机名");
        }
        try {
            validate(endpoint, Arrays.asList(InetAddress.getAllByName(endpoint.getHost())));
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("连接器端点域名无法解析", ex);
        }
    }

    public void validate(URI endpoint, List<InetAddress> resolvedAddresses) {
        if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme())) {
            throw new IllegalArgumentException("连接器端点只允许 HTTPS");
        }
        if (endpoint.getHost() == null || endpoint.getUserInfo() != null) {
            throw new IllegalArgumentException("连接器端点格式不安全");
        }
        if (resolvedAddresses == null || resolvedAddresses.isEmpty()) {
            throw new IllegalArgumentException("连接器端点没有可用地址");
        }
        if (resolvedAddresses.stream().anyMatch(this::isProtectedAddress)) {
            throw new IllegalArgumentException("连接器端点解析到受保护网络");
        }
    }

    private boolean isProtectedAddress(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isCarrierGradeNat(address);
    }

    private boolean isCarrierGradeNat(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 4
                && Byte.toUnsignedInt(bytes[0]) == 100
                && Byte.toUnsignedInt(bytes[1]) >= 64
                && Byte.toUnsignedInt(bytes[1]) <= 127;
    }
}
