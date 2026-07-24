package com.hunyuan.sa.admin.module.system.support;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.heartbeat.api.PlatformHeartbeatFacade;
import com.hunyuan.sa.base.module.support.heartbeat.api.PlatformHeartbeatPageQuery;
import com.hunyuan.sa.base.module.support.heartbeat.api.PlatformHeartbeatRecordView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台心跳记录稳定只读 HTTP 接口。
 */
@RestController
@RequestMapping("/api/admin/v1/platform/runtime/heartbeats")
@Tag(name = "平台运行时 - 心跳记录")
public class PlatformHeartbeatController {

    @Resource
    private PlatformHeartbeatFacade platformHeartbeatFacade;

    @PostMapping("/query")
    @Operation(operationId = "platformHeartbeatQuery", summary = "分页查询心跳记录")
    public ResponseDTO<PageResult<PlatformHeartbeatRecordView>> queryRecords(
            @RequestBody @Valid PlatformHeartbeatPageQuery query) {
        return platformHeartbeatFacade.queryRecords(query);
    }
}
