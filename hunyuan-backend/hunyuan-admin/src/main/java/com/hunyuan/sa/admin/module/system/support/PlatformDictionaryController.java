package com.hunyuan.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryCreateCommand;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryFacade;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryItem;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryItemCreateCommand;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryItemUpdateCommand;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryPageQuery;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionarySummary;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryUpdateCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.hunyuan.sa.base.common.domain.ValidateList;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台字典稳定 HTTP 接口。
 */
@RestController
@RequestMapping("/api/admin/v1/platform/dictionaries")
@Tag(name = "平台能力 - 字典")
public class PlatformDictionaryController {

    @Resource
    private PlatformDictionaryFacade platformDictionaryFacade;

    @PostMapping("/query")
    @Operation(operationId = "platformDictionaryQuery", summary = "分页查询字典")
    @SaCheckPermission("support:dict:query")
    public ResponseDTO<PageResult<PlatformDictionarySummary>> queryPage(
            @RequestBody @Valid PlatformDictionaryPageQuery query) {
        return ResponseDTO.ok(platformDictionaryFacade.queryPage(query));
    }

    @GetMapping
    @Operation(operationId = "platformDictionaryGetAll", summary = "查询全部可用字典")
    public ResponseDTO<List<PlatformDictionarySummary>> getAllDictionaries() {
        return ResponseDTO.ok(platformDictionaryFacade.getAllDictionaries());
    }

    @GetMapping("/items")
    @Operation(operationId = "platformDictionaryGetAllItems", summary = "查询全部字典项")
    public ResponseDTO<List<PlatformDictionaryItem>> getAllItems() {
        return ResponseDTO.ok(platformDictionaryFacade.getAllItems());
    }

    @GetMapping("/{dictionaryId}/items")
    @Operation(operationId = "platformDictionaryGetItems", summary = "查询单个字典的字典项")
    @SaCheckPermission("support:dictData:query")
    public ResponseDTO<List<PlatformDictionaryItem>> getItemsByDictionaryId(
            @PathVariable Long dictionaryId) {
        return ResponseDTO.ok(platformDictionaryFacade.getItemsByDictionaryId(dictionaryId));
    }

    @PostMapping
    @Operation(operationId = "platformDictionaryCreate", summary = "创建字典")
    @SaCheckPermission("support:dict:add")
    public ResponseDTO<String> create(@RequestBody @Valid PlatformDictionaryCreateCommand command) {
        return platformDictionaryFacade.create(command);
    }

    @PutMapping("/{dictionaryId}")
    @Operation(operationId = "platformDictionaryUpdate", summary = "更新字典")
    @SaCheckPermission("support:dict:update")
    public ResponseDTO<String> update(
            @PathVariable Long dictionaryId,
            @RequestBody @Valid PlatformDictionaryUpdateCommand command) {
        return platformDictionaryFacade.update(dictionaryId, command);
    }

    @PostMapping("/{dictionaryId}/toggle-disabled")
    @Operation(operationId = "platformDictionaryToggleDisabled", summary = "切换字典启用状态")
    @SaCheckPermission("support:dict:updateDisabled")
    public ResponseDTO<String> toggleDisabled(@PathVariable Long dictionaryId) {
        return platformDictionaryFacade.toggleDisabled(dictionaryId);
    }

    @PostMapping("/batch-delete")
    @Operation(operationId = "platformDictionaryBatchDelete", summary = "批量删除字典")
    @SaCheckPermission("support:dict:delete")
    public ResponseDTO<String> batchDelete(@RequestBody ValidateList<Long> dictionaryIds) {
        return platformDictionaryFacade.batchDelete(dictionaryIds);
    }

    @DeleteMapping("/{dictionaryId}")
    @Operation(operationId = "platformDictionaryDelete", summary = "删除字典")
    @SaCheckPermission("support:dict:delete")
    public ResponseDTO<String> delete(@PathVariable Long dictionaryId) {
        return platformDictionaryFacade.delete(dictionaryId);
    }

    @PostMapping("/{dictionaryId}/items")
    @Operation(operationId = "platformDictionaryItemCreate", summary = "创建字典项")
    @SaCheckPermission("support:dictData:add")
    public ResponseDTO<String> createItem(
            @PathVariable Long dictionaryId,
            @RequestBody @Valid PlatformDictionaryItemCreateCommand command) {
        return platformDictionaryFacade.createItem(dictionaryId, command);
    }

    @PutMapping("/{dictionaryId}/items/{dictionaryItemId}")
    @Operation(operationId = "platformDictionaryItemUpdate", summary = "更新字典项")
    @SaCheckPermission("support:dictData:update")
    public ResponseDTO<String> updateItem(
            @PathVariable Long dictionaryId,
            @PathVariable Long dictionaryItemId,
            @RequestBody @Valid PlatformDictionaryItemUpdateCommand command) {
        return platformDictionaryFacade.updateItem(dictionaryId, dictionaryItemId, command);
    }

    @PostMapping("/items/{dictionaryItemId}/toggle-disabled")
    @Operation(operationId = "platformDictionaryItemToggleDisabled", summary = "切换字典项启用状态")
    @SaCheckPermission("support:dictData:updateDisabled")
    public ResponseDTO<String> toggleItemDisabled(@PathVariable Long dictionaryItemId) {
        return platformDictionaryFacade.toggleItemDisabled(dictionaryItemId);
    }

    @PostMapping("/items/batch-delete")
    @Operation(operationId = "platformDictionaryItemBatchDelete", summary = "批量删除字典项")
    @SaCheckPermission("support:dictData:delete")
    public ResponseDTO<String> batchDeleteItems(@RequestBody ValidateList<Long> dictionaryItemIds) {
        return platformDictionaryFacade.batchDeleteItems(dictionaryItemIds);
    }

    @DeleteMapping("/items/{dictionaryItemId}")
    @Operation(operationId = "platformDictionaryItemDelete", summary = "删除字典项")
    @SaCheckPermission("support:dictData:delete")
    public ResponseDTO<String> deleteItem(@PathVariable Long dictionaryItemId) {
        return platformDictionaryFacade.deleteItem(dictionaryItemId);
    }
}
