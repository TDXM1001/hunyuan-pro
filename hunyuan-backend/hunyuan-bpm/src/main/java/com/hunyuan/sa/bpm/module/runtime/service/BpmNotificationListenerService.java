package com.hunyuan.sa.bpm.module.runtime.service;

import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmNotificationRecordVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * BPM 通知型监听器分发服务。
 */
@Service
public class BpmNotificationListenerService {

    @Resource
    private BpmNotificationRecordService notificationRecordService;

    @Resource
    private BpmNotificationChannelSender channelSender;

    /**
     * 按渠道分发通知，并为每个渠道写入 BPM 自己的投递记录。
     */
    public void dispatch(BpmNotificationCommand command) {
        for (String channel : command.safeChannels()) {
            BpmNotificationRecordVO record = notificationRecordService.createPendingRecord(command, channel);
            try {
                channelSender.send(command, channel);
                notificationRecordService.markSuccess(record.getNotificationRecordId(), "{}");
            } catch (Exception ex) {
                notificationRecordService.markFail(record.getNotificationRecordId(), ex.getMessage());
            }
        }
    }
}
