package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmAdminTaskTransferForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskDelegateForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskQueryForm;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskDetailVO;
import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmApprovalStageGovernanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程任务管理接口。
 */
@RestController
@Tag(name = "BPM Task")
public class AdminBpmTaskController {

    @Resource
    private BpmTaskService bpmTaskService;

    @Resource
    private BpmApprovalStageGovernanceService bpmApprovalStageGovernanceService;

    @Operation(summary = "分页查询流程任务")
    @PostMapping("/bpm/task/query")
    @SaCheckPermission("bpm:task:query")
    public ResponseDTO<PageResult<BpmTaskVO>> query(@RequestBody @Valid BpmTaskQueryForm queryForm) {
        return bpmTaskService.queryAdminPage(queryForm);
    }

    @Operation(summary = "查询流程任务详情")
    @GetMapping("/bpm/task/detail/{taskId}")
    @SaCheckPermission("bpm:task:detail")
    public ResponseDTO<BpmTaskDetailVO> detail(@PathVariable Long taskId) {
        return bpmTaskService.getDetail(taskId);
    }

    @Operation(summary = "管理员转交流程任务")
    @PostMapping("/bpm/task/adminTransfer")
    @SaCheckPermission("bpm:task:update")
    public ResponseDTO<String> adminTransfer(@RequestBody @Valid BpmAdminTaskTransferForm form) {
        return bpmTaskService.adminTransfer(form);
    }

    @Operation(summary = "受控转办 M2 审批阶段成员")
    @PostMapping("/bpm/task/m2MemberTransfer")
    @SaCheckPermission("bpm:task:m2-member-transfer")
    public ResponseDTO<String> m2MemberTransfer(@RequestBody @Valid BpmAdminTaskTransferForm form) {
        return bpmApprovalStageGovernanceService.transfer(
                form.getTaskId(), form.getTargetEmployeeId(), form.getReason()
        );
    }

    @Operation(summary = "管理员委派流程任务")
    @PostMapping("/bpm/task/adminDelegate")
    @SaCheckPermission("bpm:task:update")
    public ResponseDTO<String> adminDelegate(@RequestBody @Valid BpmTaskDelegateForm form) {
        return bpmTaskService.adminDelegate(form);
    }
}
