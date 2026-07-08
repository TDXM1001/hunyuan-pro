package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.base.module.support.mail.MailService;
import com.hunyuan.sa.base.module.support.message.service.MessageService;
import com.hunyuan.sa.base.module.support.sms.service.SmsService;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 复用基础模块完成 BPM 通知的实际投递。
 */
@Service
public class BpmNotificationBaseChannelSender implements BpmNotificationChannelSender {

    @Resource
    private MessageService messageService;

    @Resource
    private SmsService smsService;

    @Resource
    private MailService mailService;

    @Override
    public void send(BpmNotificationCommand command, String channel) {
        if ("MESSAGE".equals(channel)) {
            messageService.sendMessage(command.toMessageSendForm());
            return;
        }
        if ("SMS".equals(channel)) {
            smsService.send(command.toSmsSendForm());
            return;
        }
        if ("MAIL".equals(channel)) {
            try {
                mailService.sendMail(command.subject(), command.content(), List.of(), command.receiverMailList(), true);
            } catch (MessagingException ex) {
                throw new IllegalStateException("BPM 邮件通知发送失败", ex);
            }
        }
    }
}
