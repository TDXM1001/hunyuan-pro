package com.hunyuan.sa.base.module.support.job.application;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobCreateCommand;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobEnabledCommand;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobExecuteCommand;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobFacade;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobLogPageQuery;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobLogView;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobPageQuery;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobUpdateCommand;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobView;
import com.hunyuan.sa.base.module.support.job.api.SmartJobService;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobAddForm;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobEnabledUpdateForm;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobExecuteForm;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobLogQueryForm;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobLogVO;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobQueryForm;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobUpdateForm;
import com.hunyuan.sa.base.module.support.job.api.domain.SmartJobVO;
import com.hunyuan.sa.base.module.support.job.config.SmartJobAutoConfiguration;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/**
 * 平台定时任务用例实现，负责稳定 DTO 与既有调度管理服务之间的转换。
 */
@Service
@ConditionalOnBean(SmartJobAutoConfiguration.class)
public class PlatformJobApplicationService implements PlatformJobFacade {

    @Resource
    private SmartJobService smartJobService;

    @Override
    public ResponseDTO<PlatformJobView> getJob(Integer jobId) {
        ResponseDTO<SmartJobVO> response = smartJobService.queryJobInfo(jobId);
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(toJobView(response.getData()));
    }

    @Override
    public ResponseDTO<PageResult<PlatformJobView>> queryJobs(PlatformJobPageQuery query) {
        ResponseDTO<PageResult<SmartJobVO>> response = smartJobService.queryJob(
                SmartBeanUtil.copy(query, SmartJobQueryForm.class));
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(copyJobPage(response.getData()));
    }

    @Override
    public ResponseDTO<PageResult<PlatformJobLogView>> queryLogs(PlatformJobLogPageQuery query) {
        ResponseDTO<PageResult<SmartJobLogVO>> response = smartJobService.queryJobLog(
                SmartBeanUtil.copy(query, SmartJobLogQueryForm.class));
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(copyLogPage(response.getData()));
    }

    @Override
    public ResponseDTO<String> createJob(PlatformJobCreateCommand command, String operatorName) {
        SmartJobAddForm form = SmartBeanUtil.copy(command, SmartJobAddForm.class);
        form.setUpdateName(operatorName);
        return smartJobService.addJob(form);
    }

    @Override
    public ResponseDTO<String> updateJob(PlatformJobUpdateCommand command, String operatorName) {
        SmartJobUpdateForm form = SmartBeanUtil.copy(command, SmartJobUpdateForm.class);
        form.setUpdateName(operatorName);
        return smartJobService.updateJob(form);
    }

    @Override
    public ResponseDTO<String> updateEnabled(
            PlatformJobEnabledCommand command, String operatorName) {
        SmartJobEnabledUpdateForm form = SmartBeanUtil.copy(
                command, SmartJobEnabledUpdateForm.class);
        form.setUpdateName(operatorName);
        return smartJobService.updateJobEnabled(form);
    }

    @Override
    public ResponseDTO<String> executeJob(
            PlatformJobExecuteCommand command, String operatorName) {
        SmartJobExecuteForm form = SmartBeanUtil.copy(command, SmartJobExecuteForm.class);
        form.setUpdateName(operatorName);
        return smartJobService.execute(form);
    }

    @Override
    public ResponseDTO<String> deleteJob(Integer jobId, String operatorName) {
        return smartJobService.deleteJob(jobId, operatorName);
    }

    /**
     * 复制任务分页元数据并转换公开视图。
     */
    private PageResult<PlatformJobView> copyJobPage(PageResult<SmartJobVO> source) {
        PageResult<PlatformJobView> result = copyPageMetadata(source);
        result.setList(source.getList().stream().map(this::toJobView).toList());
        return result;
    }

    /**
     * 复制日志分页元数据并转换公开视图。
     */
    private PageResult<PlatformJobLogView> copyLogPage(PageResult<SmartJobLogVO> source) {
        PageResult<PlatformJobLogView> result = copyPageMetadata(source);
        result.setList(SmartBeanUtil.copyList(source.getList(), PlatformJobLogView.class));
        return result;
    }

    private PlatformJobView toJobView(SmartJobVO source) {
        PlatformJobView target = SmartBeanUtil.copy(source, PlatformJobView.class);
        if (source.getLastJobLog() != null) {
            target.setLastJobLog(SmartBeanUtil.copy(
                    source.getLastJobLog(), PlatformJobLogView.class));
        }
        return target;
    }

    /**
     * 复制分页公共元数据，避免公开边界复用历史分页元素类型。
     */
    private <S, T> PageResult<T> copyPageMetadata(PageResult<S> source) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(source.getPageNum());
        result.setPageSize(source.getPageSize());
        result.setTotal(source.getTotal());
        result.setPages(source.getPages());
        result.setEmptyFlag(source.getEmptyFlag());
        return result;
    }
}
