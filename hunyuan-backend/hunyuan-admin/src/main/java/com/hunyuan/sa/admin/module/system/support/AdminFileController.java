package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.controller.SupportBaseController;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.constant.SwaggerTagConst;
import com.hunyuan.sa.base.module.support.file.api.PlatformFileFacade;
import com.hunyuan.sa.base.module.support.file.api.PlatformFilePageQuery;
import com.hunyuan.sa.base.module.support.file.api.PlatformFileSummary;
import com.hunyuan.sa.base.module.support.file.domain.form.FileQueryForm;
import com.hunyuan.sa.base.module.support.file.domain.vo.FileVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件服务
 *
 * @Author 1024创新实验室: 罗伊
 * @Date 2019年10月11日 15:34:47
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@RestController
@Tag(name = SwaggerTagConst.Support.FILE)
public class AdminFileController extends SupportBaseController {

    @Resource
    private PlatformFileFacade platformFileFacade;

    @Operation(summary = "分页查询 @author 1024创新实验室-主任-卓大")
    @PostMapping("/file/queryPage")
    @SaCheckPermission("support:file:query")
    public ResponseDTO<PageResult<FileVO>> queryPage(@RequestBody @Valid FileQueryForm queryForm) {
        PlatformFilePageQuery query = SmartBeanUtil.copy(queryForm, PlatformFilePageQuery.class);
        PageResult<PlatformFileSummary> result = platformFileFacade.queryPage(query);
        PageResult<FileVO> legacyResult = new PageResult<>();
        legacyResult.setPageNum(result.getPageNum());
        legacyResult.setPageSize(result.getPageSize());
        legacyResult.setTotal(result.getTotal());
        legacyResult.setPages(result.getPages());
        legacyResult.setEmptyFlag(result.getEmptyFlag());
        legacyResult.setList(SmartBeanUtil.copyList(result.getList(), FileVO.class));
        return ResponseDTO.ok(legacyResult);
    }

}
