package com.hunyuan.sa.bpm.module.runtime.service;

import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import com.hunyuan.sa.base.module.support.mail.MailService;
import com.hunyuan.sa.base.module.support.message.service.MessageService;
import com.hunyuan.sa.base.module.support.sms.service.SmsService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * BPM 通知型监听器分发服务。
 */
@Service
public class BpmNotificationListenerService {

    @Resource
    private MessageService messageService;

    @Resource
    private SmsService smsService;

    @Resource
    private MailService mailService;

    /**
     * 按渠道分发站内信、短信、邮件通知。
     */
    public void dispatch(BpmNotificationCommand command) {
        if (command.safeChannels().contains("MESSAGE")) {
            messageService.sendMessage(command.toMessageSendForm());
        }
        if (command.safeChannels().contains("SMS")) {
            smsService.send(command.toSmsSendForm());
        }
        if (command.safeChannels().contains("MAIL")) {
            try {
                mailService.sendMail(command.subject(), command.content(), List.of(), command.receiverMailList(), true);
            } catch (MessagingException ex) {
                throw new IllegalStateException("BPM 邮件监听通知发送失败", ex);
            }
        }
    }
}
