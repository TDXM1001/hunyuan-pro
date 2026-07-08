package com.hunyuan.sa.bpm.controller.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskAddSignForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskApproveForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskDelegateForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskRecallForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskRejectForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskReduceSignForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskReturnForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskTransferForm;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工端流程任务接口。
 */
@RestController
@Tag(name = "App BPM Task")
public class AppBpmTaskController {

    @Resource
    private BpmTaskService bpmTaskService;

    @Operation(summary = "审批通过任务")
    @PostMapping("/app/bpm/task/approve")
    public ResponseDTO<String> approve(@RequestBody @Valid BpmTaskApproveForm approveForm) {
        return bpmTaskService.approve(approveForm);
    }

    @Operation(summary = "拒绝任务")
    @PostMapping("/app/bpm/task/reject")
    public ResponseDTO<String> reject(@RequestBody @Valid BpmTaskRejectForm rejectForm) {
        return bpmTaskService.reject(rejectForm);
    }

    @Operation(summary = "退回发起人")
    @PostMapping("/app/bpm/task/returnToInitiator")
    public ResponseDTO<String> returnToInitiator(@RequestBody @Valid BpmTaskReturnForm returnForm) {
        return bpmTaskService.returnToInitiator(returnForm);
    }

    @Operation(summary = "转办任务")
    @PostMapping("/app/bpm/task/transfer")
    public ResponseDTO<String> transfer(@RequestBody @Valid BpmTaskTransferForm transferForm) {
        return bpmTaskService.transfer(transferForm);
    }

    @Operation(summary = "委派任务")
    @PostMapping("/app/bpm/task/delegate")
    public ResponseDTO<String> delegate(@RequestBody @Valid BpmTaskDelegateForm delegateForm) {
        return bpmTaskService.delegate(delegateForm);
    }

    @Operation(summary = "加签任务")
    @PostMapping("/app/bpm/task/addSign")
    public ResponseDTO<String> addSign(@RequestBody @Valid BpmTaskAddSignForm addSignForm) {
        return bpmTaskService.addSign(addSignForm);
    }

    @Operation(summary = "减签任务")
    @PostMapping("/app/bpm/task/reduceSign")
    public ResponseDTO<String> reduceSign(@RequestBody @Valid BpmTaskReduceSignForm reduceSignForm) {
        return bpmTaskService.reduceSign(reduceSignForm);
    }

    @Operation(summary = "撤回任务")
    @PostMapping("/app/bpm/task/recall")
    public ResponseDTO<String> recall(@RequestBody @Valid BpmTaskRecallForm recallForm) {
        return bpmTaskService.recall(recallForm);
    }
}
