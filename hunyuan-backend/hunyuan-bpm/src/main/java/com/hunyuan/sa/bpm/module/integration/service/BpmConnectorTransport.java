package com.hunyuan.sa.bpm.module.integration.service;

import com.alibaba.fastjson.JSONObject;

import java.net.URI;

/**
 * 连接器 HTTP 传输边界。
 */
public interface BpmConnectorTransport {

    JSONObject invoke(URI endpoint, String method, JSONObject payload, String credential, Integer timeoutMillis);
}
