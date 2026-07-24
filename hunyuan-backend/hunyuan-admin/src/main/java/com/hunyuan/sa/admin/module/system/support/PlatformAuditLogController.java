package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.audit.api.PlatformAuditLogFacade;
import com.hunyuan.sa.base.module.support.audit.api.PlatformLoginLogPageQuery;
import com.hunyuan.sa.base.module.support.audit.api.PlatformLoginLogSummary;
import com.hunyuan.sa.base.module.support.audit.api.PlatformOperateLogPageQuery;
import com.hunyuan.sa.base.module.support.audit.api.PlatformOperateLogSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台审计日志稳定 HTTP 接口。
 */
@RestController
@RequestMapping("/api/admin/v1/platform/audit")
@Tag(name = "平台能力 - 审计日志")
public class PlatformAuditLogController {

    @Resource
    private PlatformAuditLogFacade platformAuditLogFacade;

    @PostMapping("/operation-logs/query")
    @Operation(operationId = "platformOperateLogQuery", summary = "分页查询操作日志")
    @SaCheckPermission("support:operateLog:query")
    public ResponseDTO<PageResult<PlatformOperateLogSummary>> queryOperateLogs(
            @RequestBody PlatformOperateLogPageQuery query) {
        return platformAuditLogFacade.queryOperateLogs(query);
    }

    @GetMapping("/operation-logs/{operateLogId}")
    @Operation(operationId = "platformOperateLogGet", summary = "查询操作日志详情")
    @SaCheckPermission("support:operateLog:detail")
    public ResponseDTO<PlatformOperateLogSummary> getOperateLog(@PathVariable Long operateLogId) {
        return platformAuditLogFacade.getOperateLog(operateLogId);
    }

    @PostMapping("/login-logs/query")
    @Operation(operationId = "platformLoginLogQuery", summary = "分页查询登录日志")
    @SaCheckPermission("support:loginLog:query")
    public ResponseDTO<PageResult<PlatformLoginLogSummary>> queryLoginLogs(
            @RequestBody PlatformLoginLogPageQuery query) {
        return platformAuditLogFacade.queryLoginLogs(query);
    }
}
