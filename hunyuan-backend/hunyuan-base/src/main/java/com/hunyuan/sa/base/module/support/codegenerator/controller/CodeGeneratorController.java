package com.hunyuan.sa.base.module.support.codegenerator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.controller.SupportBaseController;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.common.util.SmartResponseUtil;
import com.hunyuan.sa.base.constant.SwaggerTagConst;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorConfig;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorFacade;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorPreviewCommand;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorTablePageQuery;
import com.hunyuan.sa.base.module.support.codegenerator.api.PlatformCodeGeneratorTableView;
import com.hunyuan.sa.base.module.support.codegenerator.application.PlatformCodeGeneratorMapper;
import com.hunyuan.sa.base.module.support.codegenerator.domain.form.CodeGeneratorConfigForm;
import com.hunyuan.sa.base.module.support.codegenerator.domain.form.CodeGeneratorPreviewForm;
import com.hunyuan.sa.base.module.support.codegenerator.domain.form.TableQueryForm;
import com.hunyuan.sa.base.module.support.codegenerator.domain.vo.TableColumnVO;
import com.hunyuan.sa.base.module.support.codegenerator.domain.vo.TableConfigVO;
import com.hunyuan.sa.base.module.support.codegenerator.domain.vo.TableVO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * 代码生成
 *
 * @Author 1024创新实验室-主任: 卓大
 * @Date 2022-06-29 20:23:46
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = SwaggerTagConst.Support.CODE_GENERATOR)
@Controller
public class CodeGeneratorController extends SupportBaseController {

    @Resource
    private PlatformCodeGeneratorFacade platformCodeGeneratorFacade;

    // ------------------- 查询 -------------------

    @Operation(summary = "获取表的列 @author 卓大")
    @GetMapping("/codeGenerator/table/getTableColumns/{table}")
    @ResponseBody
    public ResponseDTO<List<TableColumnVO>> getTableColumns(@PathVariable String table) {
        var response = platformCodeGeneratorFacade.listColumns(table);
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(PlatformCodeGeneratorMapper.toLegacyColumns(response.getData()));
    }

    @Operation(summary = "查询数据库的表 @author 卓大")
    @PostMapping("/codeGenerator/table/queryTableList")
    @ResponseBody
    public ResponseDTO<PageResult<TableVO>> queryTableList(@RequestBody @Valid TableQueryForm tableQueryForm) {
        ResponseDTO<PageResult<PlatformCodeGeneratorTableView>> response =
                platformCodeGeneratorFacade.queryTables(SmartBeanUtil.copy(
                        tableQueryForm, PlatformCodeGeneratorTablePageQuery.class));
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(PlatformCodeGeneratorMapper.toLegacyTablePage(
                response.getData()));
    }

    // ------------------- 配置 -------------------

    @Operation(summary = "获取表的配置信息 @author 卓大")
    @GetMapping("/codeGenerator/table/getConfig/{table}")
    @ResponseBody
    public ResponseDTO<TableConfigVO> getTableConfig(@PathVariable String table) {
        ResponseDTO<PlatformCodeGeneratorConfig> response =
                platformCodeGeneratorFacade.getConfig(table);
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(PlatformCodeGeneratorMapper.toLegacyConfig(response.getData()));
    }

    @Operation(summary = "更新配置信息 @author 卓大")
    @PostMapping("/codeGenerator/table/updateConfig")
    @ResponseBody
    public ResponseDTO<String> updateConfig(@RequestBody @Valid CodeGeneratorConfigForm form) {
        return platformCodeGeneratorFacade.updateConfig(
                PlatformCodeGeneratorMapper.toUpdateCommand(form));
    }

    // ------------------- 生成 -------------------

    @Operation(summary = "代码预览 @author 卓大")
    @PostMapping("/codeGenerator/code/preview")
    @ResponseBody
    public ResponseDTO<String> preview(@RequestBody @Valid CodeGeneratorPreviewForm form) {
        return platformCodeGeneratorFacade.preview(SmartBeanUtil.copy(
                form, PlatformCodeGeneratorPreviewCommand.class));
    }

    @Operation(summary = "代码下载 @author 卓大")
    @GetMapping(value = "/codeGenerator/code/download/{tableName}", produces = "application/octet-stream")
    public void download(@PathVariable String tableName, HttpServletResponse response) throws IOException {

        ResponseDTO<byte[]> download = platformCodeGeneratorFacade.download(tableName);

        if (download.getOk()) {
            SmartResponseUtil.setDownloadFileHeader(response, tableName + "_code.zip", (long) download.getData().length);
            response.getOutputStream().write(download.getData());
        } else {
            SmartResponseUtil.write(response, download);
        }
    }

}
