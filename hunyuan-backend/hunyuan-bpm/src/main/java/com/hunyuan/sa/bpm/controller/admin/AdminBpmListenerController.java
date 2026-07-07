package com.hunyuan.sa.bpm.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 流程监听器管理接口。
 */
@RestController
@Tag(name = "BPM Listener")
public class AdminBpmListenerController {

    @Operation(summary = "查询监听器目录")
    @GetMapping("/bpm/listener/query")
    public ResponseDTO<List<Map<String, Object>>> query() {
        return ResponseDTO.ok(List.of(
                Map.of("listenerCode", "notify_message", "listenerName", "站内信通知", "channels", List.of("MESSAGE")),
                Map.of("listenerCode", "notify_all", "listenerName", "全渠道通知", "channels", List.of("MESSAGE", "SMS", "MAIL"))
        ));
    }

    @Operation(summary = "查询监听器渠道选项")
    @GetMapping("/bpm/listener/channelOptions")
    public ResponseDTO<List<Map<String, String>>> channelOptions() {
        return ResponseDTO.ok(List.of(
                Map.of("label", "站内信", "value", "MESSAGE"),
                Map.of("label", "短信", "value", "SMS"),
                Map.of("label", "邮件", "value", "MAIL")
        ));
    }
}
