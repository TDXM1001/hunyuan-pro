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
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberDefinition;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberFacade;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberGenerateCommand;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberRecord;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberRecordPageQuery;
import com.hunyuan.sa.base.module.support.serialnumber.domain.SerialNumberEntity;
import com.hunyuan.sa.base.module.support.serialnumber.domain.SerialNumberGenerateForm;
import com.hunyuan.sa.base.module.support.serialnumber.domain.SerialNumberRecordEntity;
import com.hunyuan.sa.base.module.support.serialnumber.domain.SerialNumberRecordQueryForm;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 单据序列号
 *
 * @Author 1024创新实验室-主任: 卓大
 * @Date 2022-03-25 21:46:07
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = SwaggerTagConst.Support.SERIAL_NUMBER)
@RestController
public class AdminSerialNumberController extends SupportBaseController {

    @Resource
    private PlatformSerialNumberFacade platformSerialNumberFacade;

    @Operation(summary = "生成单号 @author 卓大")
    @PostMapping("/serialNumber/generate")
    @SaCheckPermission("support:serialNumber:generate")
    public ResponseDTO<List<String>> generate(@RequestBody @Valid SerialNumberGenerateForm generateForm) {
        return platformSerialNumberFacade.generate(SmartBeanUtil.copy(
                generateForm, PlatformSerialNumberGenerateCommand.class));
    }

    @Operation(summary = "获取所有单号定义 @author 卓大")
    @GetMapping("/serialNumber/all")
    public ResponseDTO<List<SerialNumberEntity>> getAll() {
        ResponseDTO<List<PlatformSerialNumberDefinition>> response =
                platformSerialNumberFacade.listDefinitions();
        if (!Boolean.TRUE.equals(response.getOk())) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(SmartBeanUtil.copyList(
                response.getData(), SerialNumberEntity.class));
    }

    @Operation(summary = "获取生成记录 @author 卓大")
    @PostMapping("/serialNumber/queryRecord")
    @SaCheckPermission("support:serialNumber:record")
    public ResponseDTO<PageResult<SerialNumberRecordEntity>> queryRecord(@RequestBody @Valid SerialNumberRecordQueryForm queryForm) {
        ResponseDTO<PageResult<PlatformSerialNumberRecord>> response =
                platformSerialNumberFacade.queryRecords(SmartBeanUtil.copy(
                        queryForm, PlatformSerialNumberRecordPageQuery.class));
        if (!Boolean.TRUE.equals(response.getOk())) {
            return ResponseDTO.error(response);
        }
        PageResult<PlatformSerialNumberRecord> source = response.getData();
        PageResult<SerialNumberRecordEntity> result = new PageResult<>();
        result.setPageNum(source.getPageNum());
        result.setPageSize(source.getPageSize());
        result.setTotal(source.getTotal());
        result.setPages(source.getPages());
        result.setEmptyFlag(source.getEmptyFlag());
        result.setList(SmartBeanUtil.copyList(
                source.getList(), SerialNumberRecordEntity.class));
        return ResponseDTO.ok(result);
    }

}
