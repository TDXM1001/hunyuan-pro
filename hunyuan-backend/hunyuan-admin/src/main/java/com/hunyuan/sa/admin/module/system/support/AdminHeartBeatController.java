package com.hunyuan.sa.admin.module.system.support;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.hunyuan.sa.base.common.controller.SupportBaseController;
import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.constant.SwaggerTagConst;
import com.hunyuan.sa.base.module.support.heartbeat.api.PlatformHeartbeatFacade;
import com.hunyuan.sa.base.module.support.heartbeat.api.PlatformHeartbeatPageQuery;
import com.hunyuan.sa.base.module.support.heartbeat.api.PlatformHeartbeatRecordView;
import com.hunyuan.sa.base.module.support.heartbeat.domain.HeartBeatRecordQueryForm;
import com.hunyuan.sa.base.module.support.heartbeat.domain.HeartBeatRecordVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 心跳记录
 *
 * @Author 1024创新实验室-主任: 卓大
 * @Date 2022-01-09 20:57:24
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = SwaggerTagConst.Support.HEART_BEAT)
@RestController
public class AdminHeartBeatController extends SupportBaseController {

    @Resource
    private PlatformHeartbeatFacade platformHeartbeatFacade;

    @PostMapping("/heartBeat/query")
    @Operation(summary = "查询心跳记录 @author 卓大")
    public ResponseDTO<PageResult<HeartBeatRecordVO>> query(@RequestBody @Valid HeartBeatRecordQueryForm pageParam) {
        ResponseDTO<PageResult<PlatformHeartbeatRecordView>> response =
                platformHeartbeatFacade.queryRecords(
                        SmartBeanUtil.copy(pageParam, PlatformHeartbeatPageQuery.class));
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        PageResult<HeartBeatRecordVO> result = new PageResult<>();
        result.setPageNum(response.getData().getPageNum());
        result.setPageSize(response.getData().getPageSize());
        result.setTotal(response.getData().getTotal());
        result.setPages(response.getData().getPages());
        result.setEmptyFlag(response.getData().getEmptyFlag());
        result.setList(SmartBeanUtil.copyList(
                response.getData().getList(), HeartBeatRecordVO.class));
        return ResponseDTO.ok(result);
    }

}
