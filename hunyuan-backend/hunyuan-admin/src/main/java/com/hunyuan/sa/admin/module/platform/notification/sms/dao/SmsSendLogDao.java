package com.hunyuan.sa.admin.module.platform.notification.sms.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.admin.module.platform.notification.sms.domain.SmsSendLogEntity;
import com.hunyuan.sa.admin.module.platform.notification.sms.domain.SmsSendLogQueryForm;
import com.hunyuan.sa.admin.module.platform.notification.sms.domain.SmsSendLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SMS send log mapper.
 */
@Mapper
public interface SmsSendLogDao extends BaseMapper<SmsSendLogEntity> {

    List<SmsSendLogVO> query(Page<?> page, @Param("query") SmsSendLogQueryForm queryForm);
}
