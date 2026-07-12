package com.hunyuan.sa.bpm.module.candidate.service;

import com.alibaba.fastjson.JSON;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * 将策略 JSON 归一化为可比较、可冻结的稳定内容。
 */
public class PolicyCanonicalizer {

    public String canonicalize(String policyJson) {
        Object parsed;
        try {
            parsed = JSON.parse(policyJson);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("策略 JSON 不合法", ex);
        }
        if (!(parsed instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("策略 JSON 必须为对象");
        }
        return JSON.toJSONString(canonicalizeValue(parsed));
    }

    public String sha256(String canonicalPayload) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM 缺少 SHA-256", ex);
        }
    }

    private Object canonicalizeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new TreeMap<>();
            map.forEach((key, nestedValue) -> result.put(String.valueOf(key), canonicalizeValue(nestedValue)));
            return result;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::canonicalizeValue).toList();
        }
        if (value != null && value.getClass().isArray()) {
            ArrayList<Object> result = new ArrayList<>();
            int length = java.lang.reflect.Array.getLength(value);
            for (int index = 0; index < length; index++) {
                result.add(canonicalizeValue(java.lang.reflect.Array.get(value, index)));
            }
            return result;
        }
        return value;
    }
}
