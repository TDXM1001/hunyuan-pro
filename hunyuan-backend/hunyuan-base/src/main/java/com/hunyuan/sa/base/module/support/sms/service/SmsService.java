package com.hunyuan.sa.base.module.support.sms.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.common.util.SmartPageUtil;
import com.hunyuan.sa.base.common.util.SmartVerificationUtil;
import com.hunyuan.sa.base.module.support.redis.RedisService;
import com.hunyuan.sa.base.module.support.sms.config.SmsProperties;
import com.hunyuan.sa.base.module.support.sms.constant.SmsSendStatusEnum;
import com.hunyuan.sa.base.module.support.sms.dao.SmsSendLogDao;
import com.hunyuan.sa.base.module.support.sms.dao.SmsTemplateDao;
import com.hunyuan.sa.base.module.support.sms.domain.*;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Unified SMS entry point for business modules.
 */
@Slf4j
@Service
public class SmsService {

    private static final String SMS_IDEMPOTENT_PREFIX = "sms:idempotent:";

    private static final String SMS_RATE_LIMIT_PREFIX = "sms:rate-limit:";

    @Resource
    private SmsProvider smsProvider;

    @Resource
    private RedisService redisService;

    @Resource
    private SmsTemplateDao smsTemplateDao;

    @Resource
    private SmsSendLogDao smsSendLogDao;

    @Resource
    private SmsProperties smsProperties;

    public PageResult<SmsSendLogVO> querySendLog(SmsSendLogQueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<SmsSendLogVO> logList = smsSendLogDao.query(page, queryForm);
        return SmartPageUtil.convert2PageResult(page, logList);
    }

    public ResponseDTO<PageResult<SmsTemplateVO>> queryTemplate(SmsTemplateQueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<SmsTemplateVO> templateList = smsTemplateDao.query(page, queryForm);
        return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, templateList));
    }

    public ResponseDTO<String> addTemplate(SmsTemplateAddForm addForm) {
        String verify = SmartBeanUtil.verify(addForm);
        if (verify != null) {
            return ResponseDTO.userErrorParam(verify);
        }

        SmsTemplateEntity existTemplate = smsTemplateDao.selectById(addForm.getTemplateCode());
        if (existTemplate != null) {
            return ResponseDTO.error(UserErrorCode.ALREADY_EXIST);
        }

        SmsTemplateEntity template = SmartBeanUtil.copy(addForm, SmsTemplateEntity.class);
        if (template.getDisableFlag() == null) {
            template.setDisableFlag(false);
        }
        smsTemplateDao.insert(template);
        return ResponseDTO.ok();
    }

    public ResponseDTO<String> updateTemplate(SmsTemplateUpdateForm updateForm) {
        String verify = SmartBeanUtil.verify(updateForm);
        if (verify != null) {
            return ResponseDTO.userErrorParam(verify);
        }

        SmsTemplateEntity existTemplate = smsTemplateDao.selectById(updateForm.getTemplateCode());
        if (existTemplate == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        SmsTemplateEntity template = SmartBeanUtil.copy(updateForm, SmsTemplateEntity.class);
        if (template.getDisableFlag() == null) {
            template.setDisableFlag(false);
        }
        smsTemplateDao.updateById(template);
        return ResponseDTO.ok();
    }

    public ResponseDTO<String> updateTemplateDisabled(String templateCode, Boolean disableFlag) {
        if (StringUtils.isBlank(templateCode) || disableFlag == null) {
            return ResponseDTO.userErrorParam();
        }

        SmsTemplateEntity existTemplate = smsTemplateDao.selectById(templateCode);
        if (existTemplate == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        SmsTemplateEntity template = new SmsTemplateEntity();
        template.setTemplateCode(templateCode);
        template.setDisableFlag(disableFlag);
        smsTemplateDao.updateById(template);
        return ResponseDTO.ok();
    }

    public ResponseDTO<SmsSendResult> send(SmsSendForm sendForm) {
        if (sendForm == null) {
            return ResponseDTO.userErrorParam("sms request cannot be null");
        }

        String verify = SmartBeanUtil.verify(sendForm);
        if (verify != null) {
            return ResponseDTO.userErrorParam(verify);
        }

        if (!Pattern.matches(SmartVerificationUtil.PHONE_REGEXP, sendForm.getPhone())) {
            return ResponseDTO.userErrorParam("invalid phone number");
        }

        ResponseDTO<ResolvedSmsContent> contentResponse = resolveContent(sendForm);
        if (!Boolean.TRUE.equals(contentResponse.getOk())) {
            return ResponseDTO.userErrorParam(contentResponse.getMsg());
        }
        ResolvedSmsContent resolvedContent = contentResponse.getData();
        sendForm.setContent(resolvedContent.getContent());

        String idempotentRedisKey = buildIdempotentRedisKey(sendForm);
        if (idempotentRedisKey != null && !redisService.getLock(idempotentRedisKey, toMilliseconds(smsProperties.getIdempotentExpireSeconds()))) {
            return ResponseDTO.userErrorParam("duplicate sms request");
        }

        String rateLimitRedisKey = redisService.generateRedisKey(
                SMS_RATE_LIMIT_PREFIX,
                sendForm.getPhone() + ":" + sendForm.getTemplateCode()
        );
        if (!redisService.getLock(rateLimitRedisKey, toMilliseconds(smsProperties.getRateLimitSeconds()))) {
            rollbackIdempotentKey(idempotentRedisKey);
            return ResponseDTO.userErrorParam("sms send too frequently");
        }

        SmsSendLogEntity sendLog = buildSendLog(sendForm, resolvedContent);
        smsSendLogDao.insert(sendLog);

        ResponseDTO<SmsSendResult> sendResult;
        try {
            sendResult = smsProvider.send(sendForm);
        } catch (Throwable e) {
            log.error("sms provider send failed, templateCode={}", sendForm.getTemplateCode(), e);
            updateSendFail(sendLog, e.getMessage());
            rollbackIdempotentKey(idempotentRedisKey);
            return ResponseDTO.userErrorParam("sms provider send failed");
        }

        if (sendResult == null) {
            updateSendFail(sendLog, "sms provider returned null response");
            rollbackIdempotentKey(idempotentRedisKey);
            return ResponseDTO.userErrorParam("sms provider returned null response");
        }

        if (!Boolean.TRUE.equals(sendResult.getOk())) {
            updateSendFail(sendLog, sendResult.getMsg());
            rollbackIdempotentKey(idempotentRedisKey);
            return sendResult;
        }

        SmsSendResult result = sendResult.getData();
        if (result == null) {
            updateSendFail(sendLog, "sms provider returned empty result");
            rollbackIdempotentKey(idempotentRedisKey);
            return ResponseDTO.userErrorParam("sms provider returned empty result");
        }

        updateSendSuccess(sendLog, result);
        return sendResult;
    }

    private ResponseDTO<ResolvedSmsContent> resolveContent(SmsSendForm sendForm) {
        SmsTemplateEntity template = smsTemplateDao.selectById(sendForm.getTemplateCode());
        if (template == null) {
            if (StringUtils.isBlank(sendForm.getContent())) {
                return ResponseDTO.userErrorParam("sms template does not exist and content is blank");
            }
            ResolvedSmsContent resolvedContent = new ResolvedSmsContent();
            resolvedContent.setContent(sendForm.getContent());
            return ResponseDTO.ok(resolvedContent);
        }

        if (Boolean.TRUE.equals(template.getDisableFlag())) {
            return ResponseDTO.userErrorParam("sms template is disabled");
        }

        StringSubstitutor stringSubstitutor = new StringSubstitutor(sendForm.getTemplateParams());
        ResolvedSmsContent resolvedContent = new ResolvedSmsContent();
        resolvedContent.setTemplateContent(template.getTemplateContent());
        resolvedContent.setContent(stringSubstitutor.replace(template.getTemplateContent()));
        return ResponseDTO.ok(resolvedContent);
    }

    private SmsSendLogEntity buildSendLog(SmsSendForm sendForm, ResolvedSmsContent resolvedContent) {
        SmsSendLogEntity sendLog = new SmsSendLogEntity();
        sendLog.setPhone(sendForm.getPhone());
        sendLog.setTemplateCode(sendForm.getTemplateCode());
        sendLog.setTemplateContent(resolvedContent.getTemplateContent());
        sendLog.setTemplateParams(JSON.toJSONString(sendForm.getTemplateParams()));
        sendLog.setSendContent(sendForm.getContent());
        sendLog.setIdempotentKey(sendForm.getIdempotentKey());
        sendLog.setSendStatus(SmsSendStatusEnum.PENDING.getValue());
        return sendLog;
    }

    private void updateSendSuccess(SmsSendLogEntity sendLog, SmsSendResult sendResult) {
        sendLog.setProvider(sendResult.getProvider());
        sendLog.setRequestId(sendResult.getRequestId());
        sendLog.setSendStatus(SmsSendStatusEnum.SUCCESS.getValue());
        sendLog.setSendTime(LocalDateTime.now());
        smsSendLogDao.updateById(sendLog);
    }

    private void updateSendFail(SmsSendLogEntity sendLog, String failReason) {
        sendLog.setSendStatus(SmsSendStatusEnum.FAIL.getValue());
        sendLog.setFailReason(StringUtils.left(failReason, 500));
        sendLog.setSendTime(LocalDateTime.now());
        smsSendLogDao.updateById(sendLog);
    }

    private String buildIdempotentRedisKey(SmsSendForm sendForm) {
        if (sendForm.getIdempotentKey() == null || sendForm.getIdempotentKey().isBlank()) {
            return null;
        }
        return redisService.generateRedisKey(SMS_IDEMPOTENT_PREFIX, sendForm.getIdempotentKey());
    }

    private void rollbackIdempotentKey(String idempotentRedisKey) {
        if (idempotentRedisKey == null) {
            return;
        }
        redisService.delete(idempotentRedisKey);
    }

    private long toMilliseconds(Long seconds) {
        if (seconds == null || seconds <= 0) {
            return 1000L;
        }
        return seconds * 1000L;
    }

    @Data
    private static class ResolvedSmsContent {

        private String templateContent;

        private String content;
    }
}
