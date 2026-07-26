package com.hunyuan.sa.admin.module.platform.notification.sms.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hunyuan.sa.admin.module.platform.notification.sms.domain.SmsTemplateEntity;
import com.hunyuan.sa.admin.module.platform.notification.sms.domain.SmsTemplateQueryForm;
import com.hunyuan.sa.admin.module.platform.notification.sms.domain.SmsTemplateVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SMS template mapper.
 */
@Mapper
public interface SmsTemplateDao extends BaseMapper<SmsTemplateEntity> {

    List<SmsTemplateVO> query(Page<?> page, @Param("query") SmsTemplateQueryForm queryForm);
}
