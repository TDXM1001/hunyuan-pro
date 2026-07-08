package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmAdminInstanceCancelForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceTraceVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceTraceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程实例管理接口。
 */
@RestController
@Tag(name = "BPM Instance")
public class AdminBpmInstanceController {

    @Resource
    private BpmInstanceService bpmInstanceService;

    @Resource
    private BpmInstanceTraceService bpmInstanceTraceService;

    @Operation(summary = "分页查询流程实例")
    @PostMapping("/bpm/instance/query")
    @SaCheckPermission("bpm:instance:query")
    public ResponseDTO<PageResult<BpmInstanceVO>> query(@RequestBody @Valid BpmInstanceQueryForm queryForm) {
        return bpmInstanceService.queryAdminPage(queryForm);
    }

    @Operation(summary = "查询流程实例详情")
    @GetMapping("/bpm/instance/detail/{instanceId}")
    @SaCheckPermission("bpm:instance:detail")
    public ResponseDTO<BpmInstanceDetailVO> detail(@PathVariable Long instanceId) {
        return bpmInstanceService.getDetail(instanceId);
    }

    @Operation(summary = "查询流程实例可靠性追踪")
    @GetMapping("/bpm/instance/trace/{instanceId}")
    @SaCheckPermission("bpm:instance:detail")
    public ResponseDTO<BpmInstanceTraceVO> trace(@PathVariable Long instanceId) {
        return bpmInstanceTraceService.getTrace(instanceId);
    }

    @Operation(summary = "管理员取消流程实例")
    @PostMapping("/bpm/instance/adminCancel")
    @SaCheckPermission("bpm:instance:update")
    public ResponseDTO<String> adminCancel(@RequestBody @Valid BpmAdminInstanceCancelForm form) {
        return bpmInstanceService.adminCancel(form);
    }

    @Operation(summary = "重同步流程实例任务投影")
    @PostMapping("/bpm/instance/resyncProjection/{instanceId}")
    @SaCheckPermission("bpm:instance:update")
    public ResponseDTO<String> resyncProjection(@PathVariable Long instanceId) {
        return bpmInstanceService.resyncProjection(instanceId);
    }
}
