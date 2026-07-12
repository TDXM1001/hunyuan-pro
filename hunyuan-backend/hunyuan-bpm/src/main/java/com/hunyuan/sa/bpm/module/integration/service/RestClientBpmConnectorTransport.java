package com.hunyuan.sa.bpm.module.integration.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * 复用平台 RestClient 的受控连接器传输实现。
 */
@Component
public class RestClientBpmConnectorTransport implements BpmConnectorTransport {

    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;

    @Resource
    private RestClient restClient;

    @Override
    public JSONObject invoke(
            URI endpoint,
            String method,
            JSONObject payload,
            String credential,
            Integer timeoutMillis
    ) {
        RestClient.RequestBodySpec request = restClient
                .method(HttpMethod.valueOf(method))
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
        if (credential != null && !credential.isBlank()) {
            request.header("Authorization", "Bearer " + credential);
        }
        String response = request
                .body(payload == null ? "{}" : payload.toJSONString())
                .retrieve()
                .body(String.class);
        if (response == null || response.isBlank()) {
            return new JSONObject();
        }
        if (response.getBytes(StandardCharsets.UTF_8).length > MAX_RESPONSE_BYTES) {
            throw new IllegalStateException("连接器响应超过 1MB 限制");
        }
        Object parsed = JSON.parse(response);
        if (!(parsed instanceof JSONObject object)) {
            throw new IllegalStateException("连接器响应必须是 JSON 对象");
        }
        return object;
    }
}
