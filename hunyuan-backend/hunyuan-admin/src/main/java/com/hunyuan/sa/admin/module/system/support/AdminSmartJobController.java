package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.controller.SupportBaseController;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import com.hunyuan.sa.base.constant.SwaggerTagConst;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobCreateCommand;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobEnabledCommand;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobExecuteCommand;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobFacade;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobLogPageQuery;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobLogView;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobPageQuery;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobUpdateCommand;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobView;
import com.hunyuan.sa.base.module.support.job.api.domain.*;
import com.hunyuan.sa.base.module.support.job.config.SmartJobAutoConfiguration;
import com.hunyuan.sa.base.module.support.repeatsubmit.annoation.RepeatSubmit;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

/**
 * 定时任务 管理接口
 *
 * @author huke
 * @date 2024/6/17 20:41
 */
@Tag(name = SwaggerTagConst.Support.JOB)
@RestController
@ConditionalOnBean(SmartJobAutoConfiguration.class)
public class AdminSmartJobController extends SupportBaseController {

    @Resource
    private PlatformJobFacade platformJobFacade;

    @Operation(summary = "定时任务-立即执行 @huke")
    @PostMapping("/job/execute")
    @SaCheckPermission("support:job:execute")
    @RepeatSubmit
    public ResponseDTO<String> execute(@RequestBody @Valid SmartJobExecuteForm executeForm) {
        return platformJobFacade.executeJob(
                SmartBeanUtil.copy(executeForm, PlatformJobExecuteCommand.class),
                currentOperatorName());
    }

    @Operation(summary = "定时任务-查询详情 @huke")
    @GetMapping("/job/{jobId}")
    @SaCheckPermission("support:job:query")
    public ResponseDTO<SmartJobVO> queryJobInfo(@PathVariable Integer jobId) {
        ResponseDTO<PlatformJobView> response = platformJobFacade.getJob(jobId);
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(toLegacyJob(response.getData()));
    }

    @Operation(summary = "定时任务-分页查询 @huke")
    @PostMapping("/job/query")
    @SaCheckPermission("support:job:query")
    public ResponseDTO<PageResult<SmartJobVO>> queryJob(@RequestBody @Valid SmartJobQueryForm queryForm) {
        ResponseDTO<PageResult<PlatformJobView>> response = platformJobFacade.queryJobs(
                SmartBeanUtil.copy(queryForm, PlatformJobPageQuery.class));
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        PageResult<SmartJobVO> result = copyPageMetadata(response.getData());
        result.setList(response.getData().getList().stream().map(this::toLegacyJob).toList());
        return ResponseDTO.ok(result);
    }

    @Operation(summary = "定时任务-添加任务 @huke")
    @PostMapping("/job/add")
    @SaCheckPermission("support:job:update")
    @RepeatSubmit
    public ResponseDTO<String> addJob(@RequestBody @Valid SmartJobAddForm addForm) {
        return platformJobFacade.createJob(
                SmartBeanUtil.copy(addForm, PlatformJobCreateCommand.class),
                currentOperatorName());
    }

    @Operation(summary = "定时任务-更新-任务信息 @huke")
    @PostMapping("/job/update")
    @SaCheckPermission("support:job:update")
    @RepeatSubmit
    public ResponseDTO<String> updateJob(@RequestBody @Valid SmartJobUpdateForm updateForm) {
        return platformJobFacade.updateJob(
                SmartBeanUtil.copy(updateForm, PlatformJobUpdateCommand.class),
                currentOperatorName());
    }

    @Operation(summary = "定时任务-更新-开启状态 @huke")
    @PostMapping("/job/update/enabled")
    @SaCheckPermission("support:job:update")
    @RepeatSubmit
    public ResponseDTO<String> updateJobEnabled(@RequestBody @Valid SmartJobEnabledUpdateForm updateForm) {
        return platformJobFacade.updateEnabled(
                SmartBeanUtil.copy(updateForm, PlatformJobEnabledCommand.class),
                currentOperatorName());
    }

    @Operation(summary = "定时任务-删除 @zhuoda")
    @GetMapping("/job/delete")
    @SaCheckPermission("support:job:update")
    @RepeatSubmit
    public ResponseDTO<String> deleteJob(@RequestParam Integer jobId) {
        return platformJobFacade.deleteJob(jobId, currentOperatorName());
    }

    @Operation(summary = "定时任务-执行记录-分页查询 @huke")
    @PostMapping("/job/log/query")
    @SaCheckPermission("support:job:log:query")
    public ResponseDTO<PageResult<SmartJobLogVO>> queryJobLog(@RequestBody @Valid SmartJobLogQueryForm queryForm) {
        ResponseDTO<PageResult<PlatformJobLogView>> response = platformJobFacade.queryLogs(
                SmartBeanUtil.copy(queryForm, PlatformJobLogPageQuery.class));
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        PageResult<SmartJobLogVO> result = copyPageMetadata(response.getData());
        result.setList(SmartBeanUtil.copyList(
                response.getData().getList(), SmartJobLogVO.class));
        return ResponseDTO.ok(result);
    }

    /**
     * 将稳定任务视图转换为历史路由响应，兼容旧客户端结构。
     */
    private SmartJobVO toLegacyJob(PlatformJobView source) {
        SmartJobVO target = SmartBeanUtil.copy(source, SmartJobVO.class);
        if (source.getLastJobLog() != null) {
            target.setLastJobLog(SmartBeanUtil.copy(
                    source.getLastJobLog(), SmartJobLogVO.class));
        }
        return target;
    }

    /**
     * 复制分页元数据，避免稳定对象泄漏到历史响应类型中。
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

    private String currentOperatorName() {
        return SmartRequestUtil.getRequestUser().getUserName();
    }
}
