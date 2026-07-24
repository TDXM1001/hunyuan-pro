package com.hunyuan.sa.base.module.support.audit.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;

/**
 * 平台审计日志管理公开边界。
 *
 * <p>该边界只承载日志查询治理，不接管操作日志切面或登录日志写入链。</p>
 */
public interface PlatformAuditLogFacade {

    ResponseDTO<PageResult<PlatformOperateLogSummary>> queryOperateLogs(
            PlatformOperateLogPageQuery query);

    ResponseDTO<PlatformOperateLogSummary> getOperateLog(Long operateLogId);

    ResponseDTO<PageResult<PlatformLoginLogSummary>> queryLoginLogs(
            PlatformLoginLogPageQuery query);
}
