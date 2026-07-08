package com.hunyuan.sa.bpm.module.runtime.service;

/**
 * BPM 通知渠道发送边界。
 */
public interface BpmNotificationChannelSender {

    void send(BpmNotificationCommand command, String channel);
}
