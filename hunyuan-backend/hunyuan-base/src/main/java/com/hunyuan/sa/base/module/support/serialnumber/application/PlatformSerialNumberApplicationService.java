package com.hunyuan.sa.base.module.support.serialnumber.application;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.common.util.SmartEnumUtil;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberDefinition;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberFacade;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberGenerateCommand;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberRecord;
import com.hunyuan.sa.base.module.support.serialnumber.api.PlatformSerialNumberRecordPageQuery;
import com.hunyuan.sa.base.module.support.serialnumber.constant.SerialNumberIdEnum;
import com.hunyuan.sa.base.module.support.serialnumber.dao.SerialNumberDao;
import com.hunyuan.sa.base.module.support.serialnumber.domain.SerialNumberEntity;
import com.hunyuan.sa.base.module.support.serialnumber.domain.SerialNumberRecordEntity;
import com.hunyuan.sa.base.module.support.serialnumber.domain.SerialNumberRecordQueryForm;
import com.hunyuan.sa.base.module.support.serialnumber.service.SerialNumberRecordService;
import com.hunyuan.sa.base.module.support.serialnumber.service.SerialNumberService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 平台序列号用例实现，隔离历史实体、DAO 和生成器实现。
 */
@Service
public class PlatformSerialNumberApplicationService implements PlatformSerialNumberFacade {

    @Resource
    private SerialNumberDao serialNumberDao;

    @Resource
    private SerialNumberService serialNumberService;

    @Resource
    private SerialNumberRecordService serialNumberRecordService;

    @Override
    public ResponseDTO<List<PlatformSerialNumberDefinition>> listDefinitions() {
        List<SerialNumberEntity> definitions = serialNumberDao.selectList(null);
        return ResponseDTO.ok(SmartBeanUtil.copyList(
                definitions, PlatformSerialNumberDefinition.class));
    }

    @Override
    public ResponseDTO<PageResult<PlatformSerialNumberRecord>> queryRecords(
            PlatformSerialNumberRecordPageQuery query) {
        PageResult<SerialNumberRecordEntity> legacyPage = serialNumberRecordService.query(
                SmartBeanUtil.copy(query, SerialNumberRecordQueryForm.class));
        return ResponseDTO.ok(copyPage(legacyPage));
    }

    @Override
    public ResponseDTO<List<String>> generate(PlatformSerialNumberGenerateCommand command) {
        SerialNumberIdEnum serialNumberId = SmartEnumUtil.getEnumByValue(
                command.getSerialNumberId(), SerialNumberIdEnum.class);
        if (serialNumberId == null) {
            return ResponseDTO.userErrorParam(
                    "序列号定义不存在：" + command.getSerialNumberId());
        }
        return ResponseDTO.ok(serialNumberService.generate(
                serialNumberId, command.getCount()));
    }

    /**
     * 复制分页元数据并转换公开记录对象。
     */
    private PageResult<PlatformSerialNumberRecord> copyPage(
            PageResult<SerialNumberRecordEntity> source) {
        PageResult<PlatformSerialNumberRecord> result = new PageResult<>();
        result.setPageNum(source.getPageNum());
        result.setPageSize(source.getPageSize());
        result.setTotal(source.getTotal());
        result.setPages(source.getPages());
        result.setEmptyFlag(source.getEmptyFlag());
        result.setList(SmartBeanUtil.copyList(
                source.getList(), PlatformSerialNumberRecord.class));
        return result;
    }
}
