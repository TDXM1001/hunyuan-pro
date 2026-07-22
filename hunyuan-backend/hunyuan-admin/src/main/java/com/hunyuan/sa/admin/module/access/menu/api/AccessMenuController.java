package com.hunyuan.sa.admin.module.access.menu.api;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.domain.RequestUrlVO;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单目录稳定管理接口。
 */
@RestController
@RequestMapping("/api/admin/v1/access/menus")
@Tag(name = "访问控制 - 菜单目录")
public class AccessMenuController {

    @Resource
    private AccessMenuCatalogFacade menuCatalogFacade;

    @GetMapping
    @Operation(operationId = "accessMenuList", summary = "查询菜单列表")
    @SaCheckPermission("access.menu.read")
    public ResponseDTO<List<AccessMenu>> list() {
        return ResponseDTO.ok(menuCatalogFacade.list());
    }

    @GetMapping("/{menuId}")
    @Operation(operationId = "accessMenuGet", summary = "查询菜单详情")
    @SaCheckPermission("access.menu.read")
    public ResponseDTO<AccessMenu> get(@PathVariable Long menuId) {
        return toResponse(menuCatalogFacade.get(menuId));
    }

    @GetMapping("/tree")
    @Operation(operationId = "accessMenuTree", summary = "查询菜单树")
    @SaCheckPermission("access.menu.read")
    public ResponseDTO<List<AccessMenuNode>> tree(@RequestParam("onlyMenu") Boolean onlyMenu) {
        return ResponseDTO.ok(menuCatalogFacade.tree(onlyMenu));
    }

    @GetMapping("/authorization-urls")
    @Operation(operationId = "accessMenuAuthorizationUrlList", summary = "查询授权请求地址")
    @SaCheckPermission("access.menu.read")
    public ResponseDTO<List<RequestUrlVO>> listAuthorizationUrls() {
        return ResponseDTO.ok(menuCatalogFacade.listAuthorizationUrls());
    }

    @PostMapping
    @Operation(operationId = "accessMenuCreate", summary = "创建菜单")
    @SaCheckPermission("access.menu.create")
    public ResponseDTO<Long> create(@Valid @RequestBody AccessMenuRequest request) {
        return toResponse(menuCatalogFacade.create(request.toCreateCommand(
                SmartRequestUtil.getRequestUserId())));
    }

    @PutMapping("/{menuId}")
    @Operation(operationId = "accessMenuUpdate", summary = "更新菜单")
    @SaCheckPermission("access.menu.update")
    public ResponseDTO<String> update(
            @PathVariable Long menuId,
            @Valid @RequestBody AccessMenuRequest request) {
        return toMutationResponse(menuCatalogFacade.update(request.toUpdateCommand(
                menuId,
                SmartRequestUtil.getRequestUserId())));
    }

    @DeleteMapping
    @Operation(operationId = "accessMenuDelete", summary = "批量删除菜单")
    @SaCheckPermission("access.menu.delete")
    public ResponseDTO<String> delete(@Valid @RequestBody DeleteAccessMenusRequest request) {
        return toMutationResponse(menuCatalogFacade.delete(
                request.menuIds(),
                SmartRequestUtil.getRequestUserId()));
    }

    private ResponseDTO<String> toMutationResponse(AccessMenuResult<?> result) {
        return result.successful() ? ResponseDTO.ok() : errorResponse(result);
    }

    private <T> ResponseDTO<T> toResponse(AccessMenuResult<T> result) {
        return result.successful() ? ResponseDTO.ok(result.data()) : errorResponse(result);
    }

    private <T> ResponseDTO<T> errorResponse(AccessMenuResult<?> result) {
        if (result.failure() == AccessMenuFailure.MENU_NOT_FOUND
                || result.failure() == AccessMenuFailure.MENU_DELETED) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST, result.message());
        }
        return ResponseDTO.userErrorParam(result.message());
    }

    /**
     * 创建和更新菜单共用的请求体。
     */
    public record AccessMenuRequest(
            @NotBlank(message = "菜单名称不能为空")
            @Size(max = 30, message = "菜单名称最多30个字符")
            String menuName,
            @NotNull(message = "菜单类型不能为空")
            @Min(value = 1, message = "菜单类型错误")
            @Max(value = 3, message = "菜单类型错误")
            Integer menuType,
            @NotNull(message = "父菜单ID不能为空")
            Long parentId,
            Integer sort,
            String path,
            String component,
            @NotNull(message = "是否为外链不能为空")
            Boolean frameFlag,
            String frameUrl,
            @NotNull(message = "是否缓存不能为空")
            Boolean cacheFlag,
            @NotNull(message = "显示状态不能为空")
            Boolean visibleFlag,
            @NotNull(message = "禁用状态不能为空")
            Boolean disabledFlag,
            Integer permsType,
            String webPerms,
            String apiPerms,
            String icon,
            Long contextMenuId
    ) {

        CreateAccessMenuCommand toCreateCommand(Long operatorId) {
            return new CreateAccessMenuCommand(
                    menuName,
                    menuType,
                    parentId,
                    sort,
                    path,
                    component,
                    frameFlag,
                    frameUrl,
                    cacheFlag,
                    visibleFlag,
                    disabledFlag,
                    permsType,
                    webPerms,
                    apiPerms,
                    icon,
                    contextMenuId,
                    operatorId);
        }

        UpdateAccessMenuCommand toUpdateCommand(Long menuId, Long operatorId) {
            return new UpdateAccessMenuCommand(
                    menuId,
                    menuName,
                    menuType,
                    parentId,
                    sort,
                    path,
                    component,
                    frameFlag,
                    frameUrl,
                    cacheFlag,
                    visibleFlag,
                    disabledFlag,
                    permsType,
                    webPerms,
                    apiPerms,
                    icon,
                    contextMenuId,
                    operatorId);
        }
    }

    /**
     * 批量删除菜单请求。
     */
    public record DeleteAccessMenusRequest(
            @NotEmpty(message = "所选菜单不能为空")
            List<Long> menuIds
    ) {
    }
}
