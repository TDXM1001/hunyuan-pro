package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobCreateCommand;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobEnabledCommand;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobExecuteCommand;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobFacade;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobLogPageQuery;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobLogView;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobPageQuery;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobUpdateCommand;
import com.hunyuan.sa.base.module.support.job.api.PlatformJobView;
import com.hunyuan.sa.base.module.support.job.config.SmartJobAutoConfiguration;
import com.hunyuan.sa.base.module.support.repeatsubmit.annoation.RepeatSubmit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台定时任务稳定 HTTP 接口。
 */
@RestController
@RequestMapping("/api/admin/v1/platform/runtime/jobs")
@ConditionalOnBean(SmartJobAutoConfiguration.class)
@Tag(name = "平台运行时 - 定时任务")
public class PlatformJobController {

    @Resource
    private PlatformJobFacade platformJobFacade;

    @GetMapping("/{jobId}")
    @Operation(operationId = "platformJobDetail", summary = "查询定时任务详情")
    @SaCheckPermission("support:job:query")
    public ResponseDTO<PlatformJobView> getJob(@PathVariable Integer jobId) {
        return platformJobFacade.getJob(jobId);
    }

    @PostMapping("/query")
    @Operation(operationId = "platformJobQuery", summary = "分页查询定时任务")
    @SaCheckPermission("support:job:query")
    public ResponseDTO<PageResult<PlatformJobView>> queryJobs(
            @RequestBody @Valid PlatformJobPageQuery query) {
        return platformJobFacade.queryJobs(query);
    }

    @PostMapping("/logs/query")
    @Operation(operationId = "platformJobLogQuery", summary = "分页查询任务执行记录")
    @SaCheckPermission("support:job:log:query")
    public ResponseDTO<PageResult<PlatformJobLogView>> queryLogs(
            @RequestBody @Valid PlatformJobLogPageQuery query) {
        return platformJobFacade.queryLogs(query);
    }

    @PostMapping
    @Operation(operationId = "platformJobCreate", summary = "创建定时任务")
    @SaCheckPermission("support:job:update")
    @RepeatSubmit
    public ResponseDTO<String> createJob(
            @RequestBody @Valid PlatformJobCreateCommand command) {
        return platformJobFacade.createJob(command, currentOperatorName());
    }

    @PutMapping
    @Operation(operationId = "platformJobUpdate", summary = "更新定时任务")
    @SaCheckPermission("support:job:update")
    @RepeatSubmit
    public ResponseDTO<String> updateJob(
            @RequestBody @Valid PlatformJobUpdateCommand command) {
        return platformJobFacade.updateJob(command, currentOperatorName());
    }

    @PutMapping("/enabled")
    @Operation(operationId = "platformJobEnabledUpdate", summary = "更新任务启停状态")
    @SaCheckPermission("support:job:update")
    @RepeatSubmit
    public ResponseDTO<String> updateEnabled(
            @RequestBody @Valid PlatformJobEnabledCommand command) {
        return platformJobFacade.updateEnabled(command, currentOperatorName());
    }

    @PostMapping("/execute")
    @Operation(operationId = "platformJobExecute", summary = "立即执行定时任务")
    @SaCheckPermission("support:job:execute")
    @RepeatSubmit
    public ResponseDTO<String> executeJob(
            @RequestBody @Valid PlatformJobExecuteCommand command) {
        return platformJobFacade.executeJob(command, currentOperatorName());
    }

    @DeleteMapping("/{jobId}")
    @Operation(operationId = "platformJobDelete", summary = "删除定时任务")
    @SaCheckPermission("support:job:update")
    @RepeatSubmit
    public ResponseDTO<String> deleteJob(@PathVariable Integer jobId) {
        return platformJobFacade.deleteJob(jobId, currentOperatorName());
    }

    /**
     * 操作人只从当前登录会话获取，稳定接口不接受客户端伪造更新人。
     */
    private String currentOperatorName() {
        return SmartRequestUtil.getRequestUser().getUserName();
    }
}
