package com.hunyuan.sa.base.module.support.heartbeat.application;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.module.support.heartbeat.HeartBeatService;
import com.hunyuan.sa.base.module.support.heartbeat.api.PlatformHeartbeatFacade;
import com.hunyuan.sa.base.module.support.heartbeat.api.PlatformHeartbeatPageQuery;
import com.hunyuan.sa.base.module.support.heartbeat.api.PlatformHeartbeatRecordView;
import com.hunyuan.sa.base.module.support.heartbeat.domain.HeartBeatRecordQueryForm;
import com.hunyuan.sa.base.module.support.heartbeat.domain.HeartBeatRecordVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 平台心跳记录查询用例实现，不介入心跳线程和记录写入机制。
 */
@Service
public class PlatformHeartbeatApplicationService implements PlatformHeartbeatFacade {

    @Resource
    private HeartBeatService heartBeatService;

    @Override
    public ResponseDTO<PageResult<PlatformHeartbeatRecordView>> queryRecords(
            PlatformHeartbeatPageQuery query) {
        ResponseDTO<PageResult<HeartBeatRecordVO>> response = heartBeatService.pageQuery(
                SmartBeanUtil.copy(query, HeartBeatRecordQueryForm.class));
        if (!response.getOk()) {
            return ResponseDTO.error(response);
        }
        return ResponseDTO.ok(copyPage(response.getData()));
    }

    /**
     * 复制分页元数据并隔离历史心跳视图类型。
     */
    private PageResult<PlatformHeartbeatRecordView> copyPage(PageResult<HeartBeatRecordVO> source) {
        PageResult<PlatformHeartbeatRecordView> result = new PageResult<>();
        result.setPageNum(source.getPageNum());
        result.setPageSize(source.getPageSize());
        result.setTotal(source.getTotal());
        result.setPages(source.getPages());
        result.setEmptyFlag(source.getEmptyFlag());
        result.setList(SmartBeanUtil.copyList(source.getList(), PlatformHeartbeatRecordView.class));
        return result;
    }
}
