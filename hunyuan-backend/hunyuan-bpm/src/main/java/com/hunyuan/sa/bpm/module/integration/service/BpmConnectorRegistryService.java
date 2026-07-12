package com.hunyuan.sa.bpm.module.integration.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.module.integration.dao.BpmConnectorDefinitionDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmConnectorDefinitionEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 登记连接器及其受控操作目录。
 */
@Service
public class BpmConnectorRegistryService {

    private static final Set<String> ALLOWED_METHODS = Set.of("GET", "POST", "PUT", "PATCH");

    @Resource
    private BpmConnectorDefinitionDao bpmConnectorDefinitionDao;

    public RegisteredConnector requireOperation(String connectorKey, Integer version, String operationKey) {
        var query = Wrappers.<BpmConnectorDefinitionEntity>lambdaQuery()
                .eq(BpmConnectorDefinitionEntity::getConnectorKey, connectorKey);
        if (version != null) {
            query.eq(BpmConnectorDefinitionEntity::getConnectorVersion, version);
        } else {
            query.orderByDesc(BpmConnectorDefinitionEntity::getConnectorVersion).last("LIMIT 1");
        }
        BpmConnectorDefinitionEntity definition = bpmConnectorDefinitionDao.selectOne(query);
        if (definition == null) {
            throw new IllegalArgumentException("连接器版本不存在");
        }
        if (!"ENABLED".equals(definition.getEnabledState())) {
            throw new IllegalStateException("连接器未启用或已被紧急停用");
        }
        JSONArray operations = JSON.parseArray(definition.getAllowedOperationsJson());
        if (operations != null) {
            for (Object rawOperation : operations) {
                if (!(rawOperation instanceof JSONObject operation)
                        || !operationKey.equals(operation.getString("operationKey"))) {
                    continue;
                }
                String path = operation.getString("path");
                String method = operation.getString("method");
                validateOperationPathAndMethod(path, method);
                return new RegisteredConnector(
                        definition,
                        new RegisteredOperation(
                                operationKey,
                                path,
                                method,
                                operation.getBooleanValue("idempotent")
                        )
                );
            }
        }
        throw new IllegalArgumentException("连接器操作未登记");
    }

    private void validateOperationPathAndMethod(String path, String method) {
        if (path == null || !path.startsWith("/") || path.startsWith("//") || path.contains("..")) {
            throw new IllegalArgumentException("连接器操作路径不安全");
        }
        if (!ALLOWED_METHODS.contains(method)) {
            throw new IllegalArgumentException("连接器操作方法不受支持");
        }
    }

    public record RegisteredConnector(
            BpmConnectorDefinitionEntity definition,
            RegisteredOperation operation
    ) {
    }

    public record RegisteredOperation(
            String operationKey,
            String path,
            String method,
            boolean idempotent
    ) {
    }
}
