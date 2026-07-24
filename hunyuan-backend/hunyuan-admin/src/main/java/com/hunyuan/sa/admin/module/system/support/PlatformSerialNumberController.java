package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberDefinition;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberFacade;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberGenerateCommand;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberRecord;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberRecordPageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台序列号稳定 HTTP 接口。
 */
@RestController
@RequestMapping("/api/admin/v1/platform/runtime/serial-numbers")
@Tag(name = "平台运行时 - 序列号")
public class PlatformSerialNumberController {

    @Resource
    private PlatformSerialNumberFacade platformSerialNumberFacade;

    @GetMapping
    @Operation(operationId = "platformSerialNumberList", summary = "查询序列号定义")
    public ResponseDTO<List<PlatformSerialNumberDefinition>> listDefinitions() {
        return platformSerialNumberFacade.listDefinitions();
    }

    @PostMapping("/records/query")
    @Operation(operationId = "platformSerialNumberRecordQuery", summary = "分页查询序列号生成记录")
    @SaCheckPermission("support:serialNumber:record")
    public ResponseDTO<PageResult<PlatformSerialNumberRecord>> queryRecords(
            @RequestBody @Valid PlatformSerialNumberRecordPageQuery query) {
        return platformSerialNumberFacade.queryRecords(query);
    }

    @PostMapping("/generate")
    @Operation(operationId = "platformSerialNumberGenerate", summary = "批量生成序列号")
    @SaCheckPermission("support:serialNumber:generate")
    public ResponseDTO<List<String>> generate(
            @RequestBody @Valid PlatformSerialNumberGenerateCommand command) {
        return platformSerialNumberFacade.generate(command);
    }
}
