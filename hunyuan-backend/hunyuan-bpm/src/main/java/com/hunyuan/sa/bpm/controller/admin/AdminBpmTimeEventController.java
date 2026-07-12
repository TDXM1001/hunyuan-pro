package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmExternalWaitEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTimeEventEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmExternalWaitQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTimeEventQueryForm;
import com.hunyuan.sa.bpm.module.runtime.service.BpmExternalWaitOperationsService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTimeEventOperationsService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * BPM 时间事件与外部等待运营接口。
 */
@RestController
public class AdminBpmTimeEventController {

    @Resource
    private BpmTimeEventOperationsService bpmTimeEventOperationsService;

    @Resource
    private BpmExternalWaitOperationsService bpmExternalWaitOperationsService;

    @PostMapping("/bpm/time-event/query")
    @SaCheckPermission("bpm:time-event:query")
    public ResponseDTO<PageResult<BpmTimeEventEntity>> queryTimeEvent(@RequestBody @Valid BpmTimeEventQueryForm form) {
        return bpmTimeEventOperationsService.queryPage(form);
    }

    @PostMapping("/bpm/time-event/retry/{timeEventId}")
    @SaCheckPermission("bpm:time-event:update")
    public ResponseDTO<String> retryTimeEvent(@PathVariable Long timeEventId) {
        return bpmTimeEventOperationsService.retry(timeEventId);
    }

    @PostMapping("/bpm/external-wait/query")
    @SaCheckPermission("bpm:time-event:query")
    public ResponseDTO<PageResult<BpmExternalWaitEntity>> queryExternalWait(@RequestBody @Valid BpmExternalWaitQueryForm form) {
        return bpmExternalWaitOperationsService.queryPage(form);
    }

    @PostMapping("/bpm/external-wait/retry/{externalWaitId}")
    @SaCheckPermission("bpm:time-event:update")
    public ResponseDTO<String> retryExternalWait(@PathVariable Long externalWaitId) {
        return bpmExternalWaitOperationsService.retry(externalWaitId);
    }

    @PostMapping("/bpm/external-wait/cancel/{externalWaitId}")
    @SaCheckPermission("bpm:time-event:update")
    public ResponseDTO<String> cancelExternalWait(@PathVariable Long externalWaitId) {
        return bpmExternalWaitOperationsService.cancel(externalWaitId);
    }
}
