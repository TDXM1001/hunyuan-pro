package com.hunyuan.sa.base.module.support.job.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;

/**
 * 平台定时任务公开边界。
 *
 * <p>该边界承载任务配置、执行控制和执行日志查询，不暴露历史表单、持久化实体或调度器实现。</p>
 */
public interface PlatformJobFacade {

    ResponseDTO<PlatformJobView> getJob(Integer jobId);

    ResponseDTO<PageResult<PlatformJobView>> queryJobs(PlatformJobPageQuery query);

    ResponseDTO<PageResult<PlatformJobLogView>> queryLogs(PlatformJobLogPageQuery query);

    ResponseDTO<String> createJob(PlatformJobCreateCommand command, String operatorName);

    ResponseDTO<String> updateJob(PlatformJobUpdateCommand command, String operatorName);

    ResponseDTO<String> updateEnabled(PlatformJobEnabledCommand command, String operatorName);

    ResponseDTO<String> executeJob(PlatformJobExecuteCommand command, String operatorName);

    ResponseDTO<String> deleteJob(Integer jobId, String operatorName);
}
