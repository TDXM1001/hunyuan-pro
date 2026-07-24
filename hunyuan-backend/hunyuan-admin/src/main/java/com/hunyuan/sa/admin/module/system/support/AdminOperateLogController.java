package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import com.hunyuan.sa.base.common.controller.SupportBaseController;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.constant.SwaggerTagConst;
import com.hunyuan.sa.base.module.support.audit.api.PlatformAuditLogFacade;
import com.hunyuan.sa.base.module.support.audit.api.PlatformOperateLogPageQuery;
import com.hunyuan.sa.base.module.support.audit.api.PlatformOperateLogSummary;
import com.hunyuan.sa.base.module.support.operatelog.domain.OperateLogQueryForm;
import com.hunyuan.sa.base.module.support.operatelog.domain.OperateLogVO;
import org.springframework.web.bind.annotation.*;

/**
 *  操作日志
 *
 * @Author 1024创新实验室: 罗伊
 * @Date 2021-12-08 20:48:52
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@RestController
@Tag(name = SwaggerTagConst.Support.OPERATE_LOG)
public class AdminOperateLogController extends SupportBaseController {

    @Resource
    private PlatformAuditLogFacade platformAuditLogFacade;

    @Operation(summary = "分页查询 @author 罗伊")
    @PostMapping("/operateLog/page/query")
    @SaCheckPermission("support:operateLog:query")
    public ResponseDTO<PageResult<OperateLogVO>> queryByPage(@RequestBody OperateLogQueryForm queryForm) {
        return queryLegacyPage(queryForm);
    }

    @Operation(summary = "详情 @author 罗伊")
    @GetMapping("/operateLog/detail/{operateLogId}")
    @SaCheckPermission("support:operateLog:detail")
    public ResponseDTO<OperateLogVO> detail(@PathVariable Long operateLogId) {
        ResponseDTO<PlatformOperateLogSummary> response =
                platformAuditLogFacade.getOperateLog(operateLogId);
        if (!Boolean.TRUE.equals(response.getOk())) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(SmartBeanUtil.copy(response.getData(), OperateLogVO.class));
    }

    @Operation(summary = "分页查询当前登录人信息 @author 善逸")
    @PostMapping("/operateLog/page/query/login")
    public ResponseDTO<PageResult<OperateLogVO>> queryByPageLogin(@RequestBody OperateLogQueryForm queryForm) {
        RequestUser requestUser = SmartRequestUtil.getRequestUser();
        queryForm.setOperateUserId(requestUser.getUserId());
        queryForm.setOperateUserType(requestUser.getUserType().getValue());
        return queryLegacyPage(queryForm);
    }

    /**
     * 通过稳定审计边界查询，并保持历史分页响应对象不变。
     */
    private ResponseDTO<PageResult<OperateLogVO>> queryLegacyPage(OperateLogQueryForm queryForm) {
        PlatformOperateLogPageQuery query = SmartBeanUtil.copy(
                queryForm, PlatformOperateLogPageQuery.class);
        ResponseDTO<PageResult<PlatformOperateLogSummary>> response =
                platformAuditLogFacade.queryOperateLogs(query);
        if (!Boolean.TRUE.equals(response.getOk())) {
            return ResponseDTO.error(response);
        }
        PageResult<PlatformOperateLogSummary> source = response.getData();
        PageResult<OperateLogVO> result = new PageResult<>();
        result.setPageNum(source.getPageNum());
        result.setPageSize(source.getPageSize());
        result.setTotal(source.getTotal());
        result.setPages(source.getPages());
        result.setEmptyFlag(source.getEmptyFlag());
        result.setList(SmartBeanUtil.copyList(source.getList(), OperateLogVO.class));
        return ResponseDTO.ok(result);
    }

}
