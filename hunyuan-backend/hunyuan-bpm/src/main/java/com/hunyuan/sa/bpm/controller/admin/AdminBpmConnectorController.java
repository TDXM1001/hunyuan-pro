package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmConnectorDefinitionEntity;
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmConnectorQueryForm;
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmConnectorSaveForm;
import com.hunyuan.sa.bpm.module.integration.service.BpmConnectorDefinitionService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * BPM 登记连接器管理接口。
 */
@RestController
public class AdminBpmConnectorController {

    @Resource
    private BpmConnectorDefinitionService bpmConnectorDefinitionService;

    @PostMapping("/bpm/connector/query")
    @SaCheckPermission("bpm:connector:query")
    public ResponseDTO<PageResult<BpmConnectorDefinitionEntity>> query(@RequestBody @Valid BpmConnectorQueryForm form) {
        return bpmConnectorDefinitionService.queryPage(form);
    }

    @PostMapping("/bpm/connector/save")
    @SaCheckPermission("bpm:connector:update")
    public ResponseDTO<Long> save(@RequestBody @Valid BpmConnectorSaveForm form) {
        return bpmConnectorDefinitionService.save(form);
    }
}
