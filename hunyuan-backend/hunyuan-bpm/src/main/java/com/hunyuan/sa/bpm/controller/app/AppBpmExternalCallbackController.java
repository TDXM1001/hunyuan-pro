package com.hunyuan.sa.bpm.controller.app;

import com.hunyuan.sa.base.common.annoation.NoNeedLogin;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmExternalWaitService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 外部系统回调入口；匿名仅代表不使用员工会话，仍强制 token、HMAC 和幂等校验。
 */
@RestController
public class AppBpmExternalCallbackController {

    @Resource
    private BpmExternalWaitService bpmExternalWaitService;

    @NoNeedLogin
    @PostMapping("/app/bpm/external/callback/{callbackToken}")
    public ResponseDTO<String> callback(
            @PathVariable String callbackToken,
            @RequestHeader("X-Bpm-Signature") String signature,
            @RequestBody String payload
    ) {
        bpmExternalWaitService.resume(callbackToken, signature, payload);
        return ResponseDTO.ok();
    }
}
