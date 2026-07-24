package com.hunyuan.sa.base.module.support.message.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.controller.SupportBaseController;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartRequestUtil;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.constant.SwaggerTagConst;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageInboxFacade;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageInboxPageQuery;
import com.hunyuan.sa.base.module.support.message.api.PlatformMessageSummary;
import com.hunyuan.sa.base.module.support.message.domain.MessageQueryForm;
import com.hunyuan.sa.base.module.support.message.domain.MessageVO;
import org.springframework.web.bind.annotation.*;

/**
 * 消息
 *
 * @author luoyi
 * @date 2024/06/22 20:20
 */
@RestController
@Tag(name = SwaggerTagConst.Support.MESSAGE)
public class MessageController extends SupportBaseController {

    @Resource
    private PlatformMessageInboxFacade platformMessageInboxFacade;

    @Operation(summary = "分页查询我的消息 @luoyi")
    @PostMapping("/message/queryMyMessage")
    public ResponseDTO<PageResult<MessageVO>> query(@RequestBody @Valid MessageQueryForm queryForm) {
        RequestUser user = SmartRequestUtil.getRequestUser();
        if(user == null){
            return ResponseDTO.userErrorParam("用户未登录");
        }

        PlatformMessageInboxPageQuery query = SmartBeanUtil.copy(
                queryForm, PlatformMessageInboxPageQuery.class);
        PageResult<PlatformMessageSummary> result = platformMessageInboxFacade.queryPage(
                query, user.getUserType(), user.getUserId());
        PageResult<MessageVO> legacyResult = new PageResult<>();
        legacyResult.setPageNum(result.getPageNum());
        legacyResult.setPageSize(result.getPageSize());
        legacyResult.setTotal(result.getTotal());
        legacyResult.setPages(result.getPages());
        legacyResult.setEmptyFlag(result.getEmptyFlag());
        legacyResult.setList(SmartBeanUtil.copyList(result.getList(), MessageVO.class));
        return ResponseDTO.ok(legacyResult);
    }

    @Operation(summary = "查询未读消息数量 @luoyi")
    @GetMapping("/message/getUnreadCount")
    public ResponseDTO<Long> getUnreadCount() {
        RequestUser user = SmartRequestUtil.getRequestUser();
        if(user == null){
            return ResponseDTO.userErrorParam("用户未登录");
        }
        return ResponseDTO.ok(platformMessageInboxFacade.getUnreadCount(
                user.getUserType(), user.getUserId()));
    }

    @Operation(summary = "更新已读 @luoyi")
    @GetMapping("/message/read/{messageId}")
    public ResponseDTO<String> updateReadFlag(@PathVariable Long messageId) {
        RequestUser user = SmartRequestUtil.getRequestUser();
        if(user == null){
            return ResponseDTO.userErrorParam("用户未登录");
        }

        platformMessageInboxFacade.markRead(messageId, user.getUserType(), user.getUserId());
        return ResponseDTO.ok();
    }

}
