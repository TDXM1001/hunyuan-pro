package com.hunyuan.sa.base.module.support.audit.application;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.module.support.audit.api.PlatformAuditLogFacade;
import com.hunyuan.sa.base.module.support.audit.api.PlatformLoginLogPageQuery;
import com.hunyuan.sa.base.module.support.audit.api.PlatformLoginLogSummary;
import com.hunyuan.sa.base.module.support.audit.api.PlatformOperateLogPageQuery;
import com.hunyuan.sa.base.module.support.audit.api.PlatformOperateLogSummary;
import com.hunyuan.sa.base.module.support.loginlog.LoginLogService;
import com.hunyuan.sa.base.module.support.loginlog.domain.LoginLogQueryForm;
import com.hunyuan.sa.base.module.support.loginlog.domain.LoginLogVO;
import com.hunyuan.sa.base.module.support.operatelog.OperateLogService;
import com.hunyuan.sa.base.module.support.operatelog.domain.OperateLogQueryForm;
import com.hunyuan.sa.base.module.support.operatelog.domain.OperateLogVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 平台审计日志管理用例实现，隔离历史日志查询对象和服务。
 */
@Service
public class PlatformAuditLogApplicationService implements PlatformAuditLogFacade {

    @Resource
    private OperateLogService operateLogService;

    @Resource
    private LoginLogService loginLogService;

    @Override
    public ResponseDTO<PageResult<PlatformOperateLogSummary>> queryOperateLogs(
            PlatformOperateLogPageQuery query) {
        ResponseDTO<PageResult<OperateLogVO>> legacyResponse = operateLogService.queryByPage(
                SmartBeanUtil.copy(query, OperateLogQueryForm.class));
        if (!Boolean.TRUE.equals(legacyResponse.getOk())) {
            return ResponseDTO.error(legacyResponse);
        }
        return ResponseDTO.ok(copyPage(
                legacyResponse.getData(), PlatformOperateLogSummary.class));
    }

    @Override
    public ResponseDTO<PlatformOperateLogSummary> getOperateLog(Long operateLogId) {
        ResponseDTO<OperateLogVO> legacyResponse = operateLogService.detail(operateLogId);
        if (!Boolean.TRUE.equals(legacyResponse.getOk())) {
            return ResponseDTO.error(legacyResponse);
        }
        return ResponseDTO.ok(SmartBeanUtil.copy(
                legacyResponse.getData(), PlatformOperateLogSummary.class));
    }

    @Override
    public ResponseDTO<PageResult<PlatformLoginLogSummary>> queryLoginLogs(
            PlatformLoginLogPageQuery query) {
        ResponseDTO<PageResult<LoginLogVO>> legacyResponse = loginLogService.queryByPage(
                SmartBeanUtil.copy(query, LoginLogQueryForm.class));
        if (!Boolean.TRUE.equals(legacyResponse.getOk())) {
            return ResponseDTO.error(legacyResponse);
        }
        return ResponseDTO.ok(copyPage(
                legacyResponse.getData(), PlatformLoginLogSummary.class));
    }

    /**
     * 复制分页元数据并转换公开日志对象。
     */
    private <S, T> PageResult<T> copyPage(PageResult<S> source, Class<T> targetType) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(source.getPageNum());
        result.setPageSize(source.getPageSize());
        result.setTotal(source.getTotal());
        result.setPages(source.getPages());
        result.setEmptyFlag(source.getEmptyFlag());
        result.setList(SmartBeanUtil.copyList(source.getList(), targetType));
        return result;
    }
}
