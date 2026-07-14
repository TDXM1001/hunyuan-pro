package com.hunyuan.sa.bpm.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.operations.domain.form.BpmOperationsActionForm;
import com.hunyuan.sa.bpm.module.operations.domain.form.BpmOperationsCaseQueryForm;
import com.hunyuan.sa.bpm.module.operations.domain.form.BpmOperationsMetricQueryForm;
import com.hunyuan.sa.bpm.module.operations.domain.form.BpmOperationsRetentionEvaluateForm;
import com.hunyuan.sa.bpm.module.operations.domain.vo.BpmOperationsActionResultVO;
import com.hunyuan.sa.bpm.module.operations.domain.vo.BpmOperationsCaseVO;
import com.hunyuan.sa.bpm.module.operations.domain.vo.BpmOperationsCaseDetailVO;
import com.hunyuan.sa.bpm.module.operations.domain.vo.BpmOperationsMetricVO;
import com.hunyuan.sa.bpm.module.operations.domain.vo.BpmOperationsRetentionDecisionVO;
import com.hunyuan.sa.bpm.module.operations.service.BpmOperationsGovernanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * BPM M7 运营与治理接口。
 */
@RestController
@Tag(name = "BPM Operations Governance")
public class AdminBpmOperationsController {

    @Resource
    private BpmOperationsGovernanceService bpmOperationsGovernanceService;

    @Operation(summary = "分页查询 BPM 运营治理异常队列")
    @PostMapping("/bpm/operations/case/query")
    @SaCheckPermission("bpm:operations:query")
    public ResponseDTO<PageResult<BpmOperationsCaseVO>> queryCase(@RequestBody @Valid BpmOperationsCaseQueryForm form) {
        return bpmOperationsGovernanceService.queryCasePage(form);
    }

    @Operation(summary = "查询 BPM 运营治理工单详情")
    @PostMapping("/bpm/operations/case/detail/{operationsCaseId}")
    @SaCheckPermission("bpm:operations:query")
    public ResponseDTO<BpmOperationsCaseDetailVO> detail(@PathVariable Long operationsCaseId) {
        return bpmOperationsGovernanceService.detail(operationsCaseId);
    }

    @Operation(summary = "导出脱敏 BPM 运营治理工单")
    @PostMapping("/bpm/operations/case/export")
    @SaCheckPermission("bpm:operations:export")
    public ResponseDTO<List<BpmOperationsCaseVO>> export(@RequestBody @Valid BpmOperationsCaseQueryForm form) {
        return bpmOperationsGovernanceService.exportCases(form);
    }

    @Operation(summary = "执行 BPM 运营治理处置命令")
    @PostMapping("/bpm/operations/action/{operationsCaseId}")
    @SaCheckPermission("bpm:operations:update")
    public ResponseDTO<BpmOperationsActionResultVO> executeAction(
            @PathVariable Long operationsCaseId,
            @RequestBody @Valid BpmOperationsActionForm form
    ) {
        if ("TERMINATE".equalsIgnoreCase(form.getActionType())) {
            StpUtil.checkPermission("bpm:operations:high-risk");
        }
        if ("ARCHIVE".equalsIgnoreCase(form.getActionType())) {
            StpUtil.checkPermission("bpm:operations:archive");
        }
        return bpmOperationsGovernanceService.executeAction(operationsCaseId, form);
    }

    @Operation(summary = "查询 BPM 运营治理指标")
    @PostMapping("/bpm/operations/metrics/query")
    @SaCheckPermission("bpm:operations:query")
    public ResponseDTO<List<BpmOperationsMetricVO>> queryMetrics(@RequestBody(required = false) BpmOperationsMetricQueryForm form) {
        return bpmOperationsGovernanceService.queryMetrics(form);
    }

    @Operation(summary = "评估 BPM 运营治理归档保留")
    @PostMapping("/bpm/operations/retention/evaluate")
    @SaCheckPermission("bpm:operations:archive")
    public ResponseDTO<BpmOperationsRetentionDecisionVO> evaluateRetention(
            @RequestBody @Valid BpmOperationsRetentionEvaluateForm form
    ) {
        return bpmOperationsGovernanceService.evaluateRetention(form);
    }
}
