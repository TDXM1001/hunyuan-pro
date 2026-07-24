package com.hunyuan.sa.base.module.support.heartbeat.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 心跳记录公开视图。
 */
@Data
public class PlatformHeartbeatRecordView {

    @Schema(description = "心跳记录编号")
    private Integer heartBeatRecordId;

    @Schema(description = "项目路径")
    private String projectPath;

    @Schema(description = "服务器 IP")
    private String serverIp;

    @Schema(description = "进程号")
    private Integer processNo;

    @Schema(description = "进程启动时间")
    private Date processStartTime;

    @Schema(description = "最近心跳时间")
    private Date heartBeatTime;
}
