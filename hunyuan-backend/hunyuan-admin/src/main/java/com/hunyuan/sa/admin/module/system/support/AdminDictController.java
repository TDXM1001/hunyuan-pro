package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.controller.SupportBaseController;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.domain.ValidateList;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.constant.SwaggerTagConst;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryFacade;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryCreateCommand;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryItem;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryItemCreateCommand;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryItemUpdateCommand;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryPageQuery;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionarySummary;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryUpdateCommand;
import com.hunyuan.sa.base.module.support.dict.domain.form.*;
import com.hunyuan.sa.base.module.support.dict.domain.vo.DictDataVO;
import com.hunyuan.sa.base.module.support.dict.domain.vo.DictVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据字典 Controller
 *
 * @Author 1024创新实验室-主任-卓大
 * @Date 2025-03-25 22:25:04
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = SwaggerTagConst.Support.DICT)
@RestController
public class AdminDictController extends SupportBaseController {

    @Resource
    private PlatformDictionaryFacade platformDictionaryFacade;

    // -------------------  获取全部数据 -------------------

    @Operation(summary = "获取全部数据（供前端缓存使用） @author 1024创新实验室-主任-卓大")
    @GetMapping("/dict/getAllDictData")
    public ResponseDTO<List<DictDataVO>> getAll() {
        return ResponseDTO.ok(platformDictionaryFacade.getAllItems().stream()
                .map(this::toLegacyItem)
                .toList());
    }

    @Operation(summary = "获取所有字典code @author 1024创新实验室-主任-卓大")
    @GetMapping("/dict/getAllDict")
    public ResponseDTO<List<DictVO>> getAllDict() {
        return ResponseDTO.ok(platformDictionaryFacade.getAllDictionaries().stream()
                .map(this::toLegacyDictionary)
                .toList());
    }

    // -------------------  字典 -------------------

    @Operation(summary = "分页查询 @author 1024创新实验室-主任-卓大")
    @PostMapping("/dict/queryPage")
    @SaCheckPermission("support:dict:query")
    public ResponseDTO<PageResult<DictVO>> queryPage(@RequestBody @Valid DictQueryForm queryForm) {
        PlatformDictionaryPageQuery query = com.hunyuan.sa.base.common.util.SmartBeanUtil.copy(
                queryForm, PlatformDictionaryPageQuery.class);
        PageResult<PlatformDictionarySummary> result = platformDictionaryFacade.queryPage(query);
        PageResult<DictVO> legacyResult = new PageResult<>();
        legacyResult.setPageNum(result.getPageNum());
        legacyResult.setPageSize(result.getPageSize());
        legacyResult.setTotal(result.getTotal());
        legacyResult.setPages(result.getPages());
        legacyResult.setEmptyFlag(result.getEmptyFlag());
        legacyResult.setList(result.getList().stream().map(this::toLegacyDictionary).toList());
        return ResponseDTO.ok(legacyResult);
    }

    @Operation(summary = "添加 @author 1024创新实验室-主任-卓大")
    @PostMapping("/dict/add")
    @SaCheckPermission("support:dict:add")
    public ResponseDTO<String> add(@RequestBody @Valid DictAddForm addForm) {
        return platformDictionaryFacade.create(SmartBeanUtil.copy(addForm, PlatformDictionaryCreateCommand.class));
    }

    @Operation(summary = "更新 @author 1024创新实验室-主任-卓大")
    @PostMapping("/dict/update")
    @SaCheckPermission("support:dict:update")
    public ResponseDTO<String> update(@RequestBody @Valid DictUpdateForm updateForm) {
        return platformDictionaryFacade.update(updateForm.getDictId(),
                SmartBeanUtil.copy(updateForm, PlatformDictionaryUpdateCommand.class));
    }

    @Operation(summary = "启用/禁用 @author 1024创新实验室-主任-卓大")
    @GetMapping("/dict/updateDisabled/{dictId}")
    @SaCheckPermission("support:dict:updateDisabled")
    public ResponseDTO<String> updateDisabled(@PathVariable Long dictId) {
        return platformDictionaryFacade.toggleDisabled(dictId);
    }

    @Operation(summary = "批量删除 @author 1024创新实验室-主任-卓大")
    @PostMapping("/dict/batchDelete")
    @SaCheckPermission("support:dict:delete")
    public ResponseDTO<String> batchDelete(@RequestBody ValidateList<Long> idList) {
        return platformDictionaryFacade.batchDelete(idList);
    }

    @Operation(summary = "单个删除 @author 1024创新实验室-主任-卓大")
    @GetMapping("/dict/delete/{dictId}")
    @SaCheckPermission("support:dict:delete")
    public ResponseDTO<String> delete(@PathVariable Long dictId) {
        return platformDictionaryFacade.delete(dictId);
    }

    // -------------------  字典数据 -------------------

    @Operation(summary = "字典数据 分页查询 @author 1024创新实验室-主任-卓大")
    @GetMapping("/dict/dictData/queryDictData/{dictId}")
    @SaCheckPermission("support:dictData:query")
    public ResponseDTO<List<DictDataVO>> queryDictData(@PathVariable Long dictId) {
        return ResponseDTO.ok(platformDictionaryFacade.getItemsByDictionaryId(dictId).stream()
                .map(this::toLegacyItem)
                .toList());
    }

    @Operation(summary = "字典数据 启用/禁用 @author 1024创新实验室-主任-卓大")
    @GetMapping("/dict/dictData/updateDisabled/{dictDataId}")
    @SaCheckPermission("support:dictData:updateDisabled")
    public ResponseDTO<String> updateDictDataDisabled(@PathVariable Long dictDataId) {
        return platformDictionaryFacade.toggleItemDisabled(dictDataId);
    }

    @Operation(summary = "字典数据 添加 @author 1024创新实验室-主任-卓大")
    @PostMapping("/dict/dictData/add")
    @SaCheckPermission("support:dictData:add")
    public ResponseDTO<String> addDictData(@RequestBody @Valid DictDataAddForm addForm) {
        return platformDictionaryFacade.createItem(addForm.getDictId(),
                SmartBeanUtil.copy(addForm, PlatformDictionaryItemCreateCommand.class));
    }

    @Operation(summary = "字典数据 更新 @author 1024创新实验室-主任-卓大")
    @PostMapping("/dict/dictData/update")
    @SaCheckPermission("support:dictData:update")
    public ResponseDTO<String> updateDictData(@RequestBody @Valid DictDataUpdateForm updateForm) {
        return platformDictionaryFacade.updateItem(updateForm.getDictId(), updateForm.getDictDataId(),
                SmartBeanUtil.copy(updateForm, PlatformDictionaryItemUpdateCommand.class));
    }

    @Operation(summary = "字典数据 批量删除 @author 1024创新实验室-主任-卓大")
    @PostMapping("/dict/dictData/batchDelete")
    @SaCheckPermission("support:dictData:delete")
    public ResponseDTO<String> batchDeleteDictData(@RequestBody ValidateList<Long> idList) {
        return platformDictionaryFacade.batchDeleteItems(idList);
    }

    @Operation(summary = "字典数据 单个删除 @author 1024创新实验室-主任-卓大")
    @GetMapping("/dict/dictData/delete/{dictDataId}")
    @SaCheckPermission("support:dictData:delete")
    public ResponseDTO<String> deleteDictData(@PathVariable Long dictDataId) {
        return platformDictionaryFacade.deleteItem(dictDataId);
    }

    /**
     * 历史路由继续返回原始对象，稳定路由不再暴露该对象。
     */
    private DictVO toLegacyDictionary(PlatformDictionarySummary source) {
        return com.hunyuan.sa.base.common.util.SmartBeanUtil.copy(source, DictVO.class);
    }

    private DictDataVO toLegacyItem(PlatformDictionaryItem source) {
        return com.hunyuan.sa.base.common.util.SmartBeanUtil.copy(source, DictDataVO.class);
    }

}
