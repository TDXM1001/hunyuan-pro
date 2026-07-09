package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.form.BpmSampleExpenseCreateForm;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.vo.BpmSampleExpenseVO;
import com.hunyuan.sa.bpm.module.sampleexpense.service.BpmSampleExpenseService;
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
 * BPM 样板费用申请管理接口。
 */
@RestController
@Tag(name = "BPM Sample Expense")
public class AdminBpmSampleExpenseController {

    @Resource
    private BpmSampleExpenseService bpmSampleExpenseService;

    @Operation(summary = "创建 BPM 样板费用申请")
    @PostMapping("/bpm/sample/expense/create")
    @SaCheckPermission("bpm:integration:update")
    public ResponseDTO<Long> create(@RequestBody @Valid BpmSampleExpenseCreateForm form) {
        return bpmSampleExpenseService.create(form);
    }

    @Operation(summary = "发起 BPM 样板费用申请流程")
    @PostMapping("/bpm/sample/expense/start/{expenseId}")
    @SaCheckPermission("bpm:integration:update")
    public ResponseDTO<Long> start(@PathVariable Long expenseId) {
        return bpmSampleExpenseService.start(expenseId);
    }

    @Operation(summary = "查询 BPM 样板费用申请详情")
    @GetMapping("/bpm/sample/expense/detail/{expenseId}")
    @SaCheckPermission("bpm:integration:query")
    public ResponseDTO<BpmSampleExpenseVO> detail(@PathVariable Long expenseId) {
        return bpmSampleExpenseService.detail(expenseId);
    }

    @Operation(summary = "设置 BPM 样板费用申请下一次回调失败")
    @PostMapping("/bpm/sample/expense/markNextCallbackFailed/{expenseId}")
    @SaCheckPermission("bpm:integration:update")
    public ResponseDTO<String> markNextCallbackFailed(@PathVariable Long expenseId) {
        return bpmSampleExpenseService.markNextCallbackFailed(expenseId);
    }
}
