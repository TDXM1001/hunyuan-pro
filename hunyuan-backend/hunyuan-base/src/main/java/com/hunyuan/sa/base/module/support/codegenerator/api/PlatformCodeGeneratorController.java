package com.hunyuan.sa.base.module.support.codegenerator.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * 平台开发工具代码生成器稳定 HTTP 接口。
 */
@RestController
@RequestMapping("/api/admin/v1/platform/devtools/code-generator")
@Tag(name = "平台开发工具 - 代码生成器")
public class PlatformCodeGeneratorController {

    @Resource
    private PlatformCodeGeneratorFacade platformCodeGeneratorFacade;

    @GetMapping("/tables/{tableName}/columns")
    @Operation(operationId = "platformCodeGeneratorColumns", summary = "查询数据库表字段")
    public ResponseDTO<List<PlatformCodeGeneratorColumnView>> listColumns(
            @PathVariable String tableName) {
        return platformCodeGeneratorFacade.listColumns(tableName);
    }

    @PostMapping("/tables/query")
    @Operation(operationId = "platformCodeGeneratorTableQuery", summary = "分页查询数据库表")
    public ResponseDTO<PageResult<PlatformCodeGeneratorTableView>> queryTables(
            @RequestBody @Valid PlatformCodeGeneratorTablePageQuery query) {
        return platformCodeGeneratorFacade.queryTables(query);
    }

    @GetMapping("/tables/{tableName}/config")
    @Operation(operationId = "platformCodeGeneratorConfig", summary = "查询代码生成配置")
    public ResponseDTO<PlatformCodeGeneratorConfig> getConfig(
            @PathVariable String tableName) {
        return platformCodeGeneratorFacade.getConfig(tableName);
    }

    @PutMapping("/tables/config")
    @Operation(operationId = "platformCodeGeneratorConfigUpdate", summary = "保存代码生成配置")
    public ResponseDTO<String> updateConfig(
            @RequestBody @Valid PlatformCodeGeneratorConfigUpdateCommand command) {
        return platformCodeGeneratorFacade.updateConfig(command);
    }

    @PostMapping("/preview")
    @Operation(operationId = "platformCodeGeneratorPreview", summary = "预览生成代码")
    public ResponseDTO<String> preview(
            @RequestBody @Valid PlatformCodeGeneratorPreviewCommand command) {
        return platformCodeGeneratorFacade.preview(command);
    }

    @GetMapping(value = "/download/{tableName}", produces = "application/octet-stream")
    @Operation(operationId = "platformCodeGeneratorDownload", summary = "下载生成代码")
    public void download(
            @PathVariable String tableName,
            HttpServletResponse response) throws IOException {
        ResponseDTO<byte[]> download = platformCodeGeneratorFacade.download(tableName);
        if (download.getOk()) {
            SmartResponseUtil.setDownloadFileHeader(
                    response, tableName + "_code.zip", (long) download.getData().length);
            response.getOutputStream().write(download.getData());
            return;
        }
        SmartResponseUtil.write(response, download);
    }
}
