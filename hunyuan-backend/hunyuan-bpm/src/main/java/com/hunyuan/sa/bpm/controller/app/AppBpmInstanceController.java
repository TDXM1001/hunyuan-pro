package com.hunyuan.sa.bpm.controller.app;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceCancelForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceCopyQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmInstanceResubmitForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceCopyVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmInstanceTraceVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmRuntimeStartDraftVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceCopyService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceTraceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工端流程实例与待办接口。
 */
@RestController
@Tag(name = "App BPM Instance")
public class AppBpmInstanceController {

    @Resource
    private BpmInstanceService bpmInstanceService;

    @Resource
    private BpmTaskService bpmTaskService;

    @Resource
    private BpmInstanceCopyService bpmInstanceCopyService;

    @Resource
    private BpmInstanceTraceService bpmInstanceTraceService;

    @Operation(summary = "查询我发起的流程实例")
    @PostMapping("/app/bpm/my-instance")
    public ResponseDTO<PageResult<BpmInstanceVO>> queryMyInstance(@RequestBody @Valid BpmInstanceQueryForm queryForm) {
        return bpmInstanceService.queryMyInstancePage(queryForm);
    }

    @Operation(summary = "取消我发起的流程实例")
    @PostMapping("/app/bpm/instance/cancel")
    public ResponseDTO<String> cancel(@RequestBody @Valid BpmInstanceCancelForm cancelForm) {
        return bpmInstanceService.cancelMyInstance(cancelForm);
    }

    @Operation(summary = "查询重提草稿")
    @GetMapping("/app/bpm/resubmit-draft/{instanceId}")
    public ResponseDTO<BpmRuntimeStartDraftVO> resubmitDraft(@PathVariable Long instanceId) {
        return bpmInstanceService.getResubmitDraft(instanceId);
    }

    @Operation(summary = "重新提交我发起的流程实例")
    @PostMapping("/app/bpm/instance/resubmit")
    public ResponseDTO<Long> resubmit(@RequestBody @Valid BpmInstanceResubmitForm resubmitForm) {
        return bpmInstanceService.resubmitMyInstance(resubmitForm);
    }

    @Operation(summary = "查询流程实例详情")
    @GetMapping("/app/bpm/instance/detail/{instanceId}")
    public ResponseDTO<BpmInstanceDetailVO> detail(@PathVariable Long instanceId) {
        return bpmInstanceService.getDetail(instanceId);
    }

    @Operation(summary = "查询员工安全流程追踪")
    @GetMapping("/app/bpm/instance/trace/{instanceId}")
    public ResponseDTO<BpmInstanceTraceVO> trace(@PathVariable Long instanceId) {
        return bpmInstanceTraceService.getEmployeeTrace(instanceId);
    }

    @Operation(summary = "查询我的抄送")
    @PostMapping("/app/bpm/my-copy")
    public ResponseDTO<PageResult<BpmInstanceCopyVO>> queryMyCopy(@RequestBody @Valid BpmInstanceCopyQueryForm queryForm) {
        return bpmInstanceCopyService.queryMyCopyPage(queryForm);
    }

    @Operation(summary = "标记我的抄送已读")
    @PostMapping("/app/bpm/copy/read/{copyId}")
    public ResponseDTO<String> markCopyRead(@PathVariable Long copyId) {
        return bpmInstanceCopyService.markRead(copyId);
    }

    @Operation(summary = "查询我的待办任务")
    @PostMapping("/app/bpm/my-todo")
    public ResponseDTO<PageResult<BpmTaskVO>> queryMyTodo(@RequestBody @Valid BpmTaskQueryForm queryForm) {
        return bpmTaskService.queryMyTodoPage(queryForm);
    }

    @Operation(summary = "查询我的已办任务")
    @PostMapping("/app/bpm/my-done")
    public ResponseDTO<PageResult<BpmTaskVO>> queryMyDone(@RequestBody @Valid BpmTaskQueryForm queryForm) {
        return bpmTaskService.queryMyDonePage(queryForm);
    }
}
