package com.hunyuan.sa.bpm.module.operations.service;

import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.module.integration.domain.form.BpmCallbackCompensateForm;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackService;
import com.hunyuan.sa.bpm.module.operations.domain.entity.BpmOperationsCaseEntity;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmAdminInstanceCancelForm;
import com.hunyuan.sa.bpm.module.runtime.service.BpmExternalWaitOperationsService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmInstanceService;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTimeEventOperationsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 将 M7 治理命令路由到拥有运行事实的 M4-M6 服务。
 */
@Service
public class BpmOperationsActionExecutor {

    @Resource
    private BpmTimeEventOperationsService bpmTimeEventOperationsService;

    @Resource
    private BpmExternalWaitOperationsService bpmExternalWaitOperationsService;

    @Resource
    private BpmBusinessCallbackService bpmBusinessCallbackService;

    @Resource
    private BpmInstanceService bpmInstanceService;

    public ResponseDTO<String> execute(BpmOperationsCaseEntity operationsCase, String actionType, String reason) {
        return switch (actionType) {
            case "RETRY" -> retry(operationsCase);
            case "COMPENSATE" -> compensate(operationsCase, reason);
            case "TERMINATE" -> terminate(operationsCase, reason);
            case "ARCHIVE" -> ResponseDTO.ok();
            default -> ResponseDTO.userErrorParam("不支持的运营治理动作");
        };
    }

    private ResponseDTO<String> retry(BpmOperationsCaseEntity operationsCase) {
        if (!Boolean.TRUE.equals(operationsCase.getRetryableFlag())) {
            return ResponseDTO.userErrorParam("当前异常不支持重试");
        }
        return switch (operationsCase.getSourceType()) {
            case "TIME_EVENT" -> bpmTimeEventOperationsService.retry(operationsCase.getSourceId());
            case "EXTERNAL_WAIT" -> bpmExternalWaitOperationsService.retry(operationsCase.getSourceId());
            case "CALLBACK" -> bpmBusinessCallbackService.retry(operationsCase.getSourceId());
            default -> ResponseDTO.userErrorParam("当前异常没有登记安全重试路径");
        };
    }

    private ResponseDTO<String> compensate(BpmOperationsCaseEntity operationsCase, String reason) {
        if (!Boolean.TRUE.equals(operationsCase.getCompensableFlag()) || !"CALLBACK".equals(operationsCase.getSourceType())) {
            return ResponseDTO.userErrorParam("当前异常不支持补偿");
        }
        BpmCallbackCompensateForm form = new BpmCallbackCompensateForm();
        form.setReason(reason);
        return bpmBusinessCallbackService.compensate(operationsCase.getSourceId(), form);
    }

    private ResponseDTO<String> terminate(BpmOperationsCaseEntity operationsCase, String reason) {
        if (operationsCase.getInstanceId() == null) {
            return ResponseDTO.userErrorParam("当前异常未关联可终止的流程实例");
        }
        BpmAdminInstanceCancelForm form = new BpmAdminInstanceCancelForm();
        form.setInstanceId(operationsCase.getInstanceId());
        form.setCancelReason(reason);
        return bpmInstanceService.adminCancel(form);
    }
}
