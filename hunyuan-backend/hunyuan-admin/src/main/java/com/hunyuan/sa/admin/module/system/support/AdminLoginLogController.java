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
import com.hunyuan.sa.base.module.support.audit.api.PlatformLoginLogPageQuery;
import com.hunyuan.sa.base.module.support.audit.api.PlatformLoginLogSummary;
import com.hunyuan.sa.base.module.support.loginlog.domain.LoginLogQueryForm;
import com.hunyuan.sa.base.module.support.loginlog.domain.LoginLogVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录日志
 *
 * @Author 1024创新实验室-主任: 卓大
 * @Date 2022/07/22 19:46:23
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@RestController
@Tag(name = SwaggerTagConst.Support.LOGIN_LOG)
public class AdminLoginLogController extends SupportBaseController {

    @Resource
    private PlatformAuditLogFacade platformAuditLogFacade;

    @Operation(summary = "分页查询 @author 卓大")
    @PostMapping("/loginLog/page/query")
    @SaCheckPermission("support:loginLog:query")
    public ResponseDTO<PageResult<LoginLogVO>> queryByPage(@RequestBody LoginLogQueryForm queryForm) {
        return queryLegacyPage(queryForm);
    }

    @Operation(summary = "分页查询当前登录人信息 @author 善逸")
    @PostMapping("/loginLog/page/query/login")
    public ResponseDTO<PageResult<LoginLogVO>> queryByPageLogin(@RequestBody LoginLogQueryForm queryForm) {
        RequestUser requestUser = SmartRequestUtil.getRequestUser();
        queryForm.setUserId(requestUser.getUserId());
        queryForm.setUserType(requestUser.getUserType().getValue());
        return queryLegacyPage(queryForm);
    }

    /**
     * 通过稳定审计边界查询，并保持历史分页响应对象不变。
     */
    private ResponseDTO<PageResult<LoginLogVO>> queryLegacyPage(LoginLogQueryForm queryForm) {
        PlatformLoginLogPageQuery query = SmartBeanUtil.copy(
                queryForm, PlatformLoginLogPageQuery.class);
        ResponseDTO<PageResult<PlatformLoginLogSummary>> response =
                platformAuditLogFacade.queryLoginLogs(query);
        if (!Boolean.TRUE.equals(response.getOk())) {
            return ResponseDTO.error(response);
        }
        PageResult<PlatformLoginLogSummary> source = response.getData();
        PageResult<LoginLogVO> result = new PageResult<>();
        result.setPageNum(source.getPageNum());
        result.setPageSize(source.getPageSize());
        result.setTotal(source.getTotal());
        result.setPages(source.getPages());
        result.setEmptyFlag(source.getEmptyFlag());
        result.setList(SmartBeanUtil.copyList(source.getList(), LoginLogVO.class));
        return ResponseDTO.ok(result);
    }


}
