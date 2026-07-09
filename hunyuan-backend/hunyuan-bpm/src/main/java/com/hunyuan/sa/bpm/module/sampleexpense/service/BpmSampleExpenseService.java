package com.hunyuan.sa.bpm.module.sampleexpense.service;

import com.alibaba.fastjson.JSON;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.bpm.api.business.BpmBusinessProcessApi;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessStartCommand;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackContext;
import com.hunyuan.sa.bpm.module.integration.service.BpmBusinessCallbackResult;
import com.hunyuan.sa.bpm.module.sampleexpense.dao.BpmSampleExpenseDao;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.entity.BpmSampleExpenseEntity;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.form.BpmSampleExpenseCreateForm;
import com.hunyuan.sa.bpm.module.sampleexpense.domain.vo.BpmSampleExpenseVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BPM 样板费用申请服务。
 */
@Service
public class BpmSampleExpenseService {

    public static final String BUSINESS_TYPE = "sample_expense";

    private static final String DEFINITION_KEY = "sample_expense_apply";

    private static final int STATUS_DRAFT = 0;

    private static final int STATUS_APPROVING = 1;

    private static final int STATUS_APPROVED = 2;

    private static final int STATUS_REJECTED = 3;

    @Resource
    private BpmSampleExpenseDao bpmSampleExpenseDao;

    @Resource
    private BpmBusinessProcessApi bpmBusinessProcessApi;

    public ResponseDTO<Long> create(BpmSampleExpenseCreateForm form) {
        BpmSampleExpenseEntity entity = new BpmSampleExpenseEntity();
        entity.setTitle(form.getTitle().trim());
        entity.setAmount(form.getAmount());
        entity.setApplicantEmployeeId(form.getApplicantEmployeeId());
        entity.setApprovalStatus(STATUS_DRAFT);
        entity.setCallbackFailFlag(false);
        bpmSampleExpenseDao.insert(entity);
        return ResponseDTO.ok(entity.getExpenseId());
    }

    public ResponseDTO<Long> start(Long expenseId) {
        BpmSampleExpenseEntity entity = bpmSampleExpenseDao.selectById(expenseId);
        if (entity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        if (entity.getInstanceId() != null) {
            return ResponseDTO.ok(entity.getInstanceId());
        }
        if (STATUS_APPROVED == entity.getApprovalStatus() || STATUS_REJECTED == entity.getApprovalStatus()) {
            return ResponseDTO.userErrorParam("样板费用申请已终态，不能再次发起");
        }

        BpmBusinessStartCommand command = new BpmBusinessStartCommand();
        command.setBusinessType(BUSINESS_TYPE);
        command.setBusinessId(entity.getExpenseId());
        command.setBusinessKey(BUSINESS_TYPE + ":" + entity.getExpenseId());
        command.setDefinitionKey(DEFINITION_KEY);
        command.setStartEmployeeId(entity.getApplicantEmployeeId());
        command.setTitle(entity.getTitle());
        command.setSummary("金额：" + entity.getAmount() + "，申请人：" + entity.getApplicantEmployeeId());
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("expenseId", entity.getExpenseId());
        formData.put("amount", entity.getAmount());
        command.setFormDataJson(JSON.toJSONString(formData));
        Long instanceId = bpmBusinessProcessApi.start(command);

        BpmSampleExpenseEntity update = new BpmSampleExpenseEntity();
        update.setExpenseId(entity.getExpenseId());
        update.setInstanceId(instanceId);
        update.setApprovalStatus(STATUS_APPROVING);
        bpmSampleExpenseDao.updateById(update);
        return ResponseDTO.ok(instanceId);
    }

    public ResponseDTO<BpmSampleExpenseVO> detail(Long expenseId) {
        BpmSampleExpenseEntity entity = bpmSampleExpenseDao.selectById(expenseId);
        if (entity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        return ResponseDTO.ok(toVO(entity));
    }

    public ResponseDTO<String> markNextCallbackFailed(Long expenseId) {
        BpmSampleExpenseEntity entity = bpmSampleExpenseDao.selectById(expenseId);
        if (entity == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }
        BpmSampleExpenseEntity update = new BpmSampleExpenseEntity();
        update.setExpenseId(expenseId);
        update.setCallbackFailFlag(true);
        bpmSampleExpenseDao.updateById(update);
        return ResponseDTO.ok();
    }

    public BpmBusinessCallbackResult handleCallback(BpmBusinessCallbackContext context) {
        BpmSampleExpenseEntity entity = bpmSampleExpenseDao.selectById(context.businessId());
        if (entity == null) {
            return BpmBusinessCallbackResult.failed("样板费用申请不存在: " + context.businessId(), null);
        }
        BpmBusinessResultEvent event;
        try {
            event = JSON.parseObject(context.requestPayloadJson(), BpmBusinessResultEvent.class);
        } catch (RuntimeException ex) {
            return BpmBusinessCallbackResult.failed("样板费用申请回调载荷解析失败", ex.getClass().getSimpleName());
        }
        if (event == null || event.getResultState() == null) {
            return BpmBusinessCallbackResult.failed("样板费用申请回调缺少审批结果", context.requestPayloadJson());
        }
        if (StringUtils.hasText(entity.getCallbackEventId())
                && entity.getCallbackEventId().equals(context.eventId())) {
            return BpmBusinessCallbackResult.success("{\"idempotent\":true}");
        }
        if (isTerminal(entity.getApprovalStatus())) {
            if (matchesTerminalResult(entity.getApprovalStatus(), event.getResultState())) {
                return BpmBusinessCallbackResult.success("{\"terminalRepeated\":true}");
            }
            return BpmBusinessCallbackResult.failed("样板费用申请已终态且结果冲突", context.requestPayloadJson());
        }
        if (Boolean.TRUE.equals(entity.getCallbackFailFlag())) {
            BpmSampleExpenseEntity update = new BpmSampleExpenseEntity();
            update.setExpenseId(entity.getExpenseId());
            update.setCallbackFailFlag(false);
            bpmSampleExpenseDao.updateById(update);
            return BpmBusinessCallbackResult.failed("样板费用申请模拟回调失败", "{\"failOnce\":true}");
        }
        if (Integer.valueOf(1).equals(event.getResultState())) {
            updateResult(entity.getExpenseId(), STATUS_APPROVED, context.eventId(), true);
            return BpmBusinessCallbackResult.success("{\"approvalStatus\":2}");
        }
        if (Integer.valueOf(2).equals(event.getResultState())) {
            updateResult(entity.getExpenseId(), STATUS_REJECTED, context.eventId(), false);
            return BpmBusinessCallbackResult.success("{\"approvalStatus\":3}");
        }
        return BpmBusinessCallbackResult.failed("未知审批结果: " + event.getResultState(), context.requestPayloadJson());
    }

    private void updateResult(Long expenseId, int approvalStatus, String eventId, boolean approved) {
        BpmSampleExpenseEntity update = new BpmSampleExpenseEntity();
        update.setExpenseId(expenseId);
        update.setApprovalStatus(approvalStatus);
        update.setCallbackEventId(eventId);
        if (approved) {
            update.setApprovedAt(LocalDateTime.now());
        } else {
            update.setRejectedAt(LocalDateTime.now());
        }
        bpmSampleExpenseDao.updateById(update);
    }

    private boolean isTerminal(Integer status) {
        return Integer.valueOf(STATUS_APPROVED).equals(status) || Integer.valueOf(STATUS_REJECTED).equals(status);
    }

    private boolean matchesTerminalResult(Integer approvalStatus, Integer resultState) {
        return (Integer.valueOf(STATUS_APPROVED).equals(approvalStatus) && Integer.valueOf(1).equals(resultState))
                || (Integer.valueOf(STATUS_REJECTED).equals(approvalStatus) && Integer.valueOf(2).equals(resultState));
    }

    private BpmSampleExpenseVO toVO(BpmSampleExpenseEntity entity) {
        BpmSampleExpenseVO vo = new BpmSampleExpenseVO();
        vo.setExpenseId(entity.getExpenseId());
        vo.setTitle(entity.getTitle());
        vo.setAmount(entity.getAmount());
        vo.setApplicantEmployeeId(entity.getApplicantEmployeeId());
        vo.setApprovalStatus(entity.getApprovalStatus());
        vo.setInstanceId(entity.getInstanceId());
        vo.setCallbackEventId(entity.getCallbackEventId());
        vo.setCallbackFailFlag(entity.getCallbackFailFlag());
        vo.setApprovedAt(entity.getApprovedAt());
        vo.setRejectedAt(entity.getRejectedAt());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
