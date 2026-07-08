package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.module.support.message.constant.MessageTypeEnum;
import com.hunyuan.sa.base.module.support.message.domain.MessageSendForm;
import com.hunyuan.sa.base.module.support.sms.domain.SmsSendForm;

import java.util.Collections;
import java.util.List;

/**
 * BPM 通知监听命令。
 */
public record BpmNotificationCommand(
        List<String> channels,
        Long instanceId,
        Long taskId,
        Long definitionId,
        Long definitionNodeId,
        String eventKey,
        Long receiverEmployeeId,
        String receiverSnapshotJson,
        String receiverPhone,
        List<String> receiverMailList,
        String title,
        String subject,
        String content,
        String smsTemplateCode
) {

    public MessageSendForm toMessageSendForm() {
        MessageSendForm sendForm = new MessageSendForm();
        sendForm.setMessageType(MessageTypeEnum.MAIL.getValue());
        sendForm.setReceiverUserType(UserTypeEnum.ADMIN_EMPLOYEE.getValue());
        sendForm.setReceiverUserId(receiverEmployeeId);
        sendForm.setTitle(title);
        sendForm.setContent(content);
        sendForm.setDataId(receiverEmployeeId);
        return sendForm;
    }

    public SmsSendForm toSmsSendForm() {
        SmsSendForm sendForm = new SmsSendForm();
        sendForm.setPhone(receiverPhone);
        sendForm.setTemplateCode(smsTemplateCode == null ? "bpm_notify" : smsTemplateCode);
        sendForm.setContent(content);
        sendForm.setIdempotentKey(instanceId + ":" + taskId + ":" + receiverEmployeeId);
        return sendForm;
    }

    public List<String> safeChannels() {
        return channels == null ? Collections.emptyList() : channels;
    }
}
