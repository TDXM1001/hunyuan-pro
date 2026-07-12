package com.hunyuan.sa.bpm.module.integration.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.common.util.SmartPageUtil;
import com.hunyuan.sa.bpm.module.integration.dao.BpmConnectorDefinitionDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmConnectorDefinitionEntity;
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmConnectorQueryForm;
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmConnectorSaveForm;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 登记连接器定义管理。
 */
@Service
public class BpmConnectorDefinitionService {

    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Set<String> STATES = Set.of("ENABLED", "DISABLED", "EMERGENCY_DISABLED");

    @Resource
    private BpmConnectorDefinitionDao bpmConnectorDefinitionDao;

    public ResponseDTO<PageResult<BpmConnectorDefinitionEntity>> queryPage(BpmConnectorQueryForm form) {
        Page<BpmConnectorDefinitionEntity> page = (Page<BpmConnectorDefinitionEntity>) SmartPageUtil.convert2PageQuery(form);
        var query = Wrappers.<BpmConnectorDefinitionEntity>lambdaQuery()
                .like(StringUtils.hasText(form.getConnectorKey()), BpmConnectorDefinitionEntity::getConnectorKey, form.getConnectorKey())
                .like(StringUtils.hasText(form.getConnectorName()), BpmConnectorDefinitionEntity::getConnectorName, form.getConnectorName())
                .eq(StringUtils.hasText(form.getEnabledState()), BpmConnectorDefinitionEntity::getEnabledState, form.getEnabledState())
                .orderByAsc(BpmConnectorDefinitionEntity::getConnectorKey)
                .orderByDesc(BpmConnectorDefinitionEntity::getConnectorVersion);
        bpmConnectorDefinitionDao.selectPage(page, query);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, page.getRecords()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<Long> save(BpmConnectorSaveForm form) {
        String validationError = validate(form);
        if (validationError != null) {
            return ResponseDTO.userErrorParam(validationError);
        }
        BpmConnectorDefinitionEntity entity = SmartBeanUtil.copy(form, BpmConnectorDefinitionEntity.class);
        if (form.getConnectorDefinitionId() == null) {
            Long count = bpmConnectorDefinitionDao.selectCount(Wrappers.<BpmConnectorDefinitionEntity>lambdaQuery()
                    .eq(BpmConnectorDefinitionEntity::getConnectorKey, form.getConnectorKey())
                    .eq(BpmConnectorDefinitionEntity::getConnectorVersion, form.getConnectorVersion()));
            if (count != null && count > 0) {
                return ResponseDTO.userErrorParam("连接器版本已存在");
            }
            bpmConnectorDefinitionDao.insert(entity);
        } else {
            bpmConnectorDefinitionDao.updateById(entity);
        }
        return ResponseDTO.ok(entity.getConnectorDefinitionId());
    }

    private String validate(BpmConnectorSaveForm form) {
        if (!SAFE_KEY.matcher(form.getConnectorKey()).matches()) {
            return "连接器编码格式非法";
        }
        if (!form.getBaseEndpointRef().startsWith("env:")
                || (form.getCredentialRef() != null && !form.getCredentialRef().startsWith("env:"))) {
            return "端点和凭据必须使用 env: 安全引用";
        }
        if (!STATES.contains(form.getEnabledState())) {
            return "连接器状态不受支持";
        }
        try {
            if (!(JSON.parse(form.getAllowedOperationsJson()) instanceof com.alibaba.fastjson.JSONArray)
                    || !(JSON.parse(form.getRequestSchemaJson()) instanceof com.alibaba.fastjson.JSONObject)
                    || !(JSON.parse(form.getResponseSchemaJson()) instanceof com.alibaba.fastjson.JSONObject)
                    || !(JSON.parse(form.getRetryPolicyJson()) instanceof com.alibaba.fastjson.JSONObject)) {
                return "连接器 JSON 契约格式不正确";
            }
        } catch (Exception ex) {
            return "连接器 JSON 契约格式不正确";
        }
        return null;
    }
}
