package com.hunyuan.sa.base.module.support.dict.application;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryFacade;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryCreateCommand;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryItem;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryItemCreateCommand;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryItemUpdateCommand;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryPageQuery;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionarySummary;
import com.hunyuan.sa.base.module.support.dict.api.PlatformDictionaryUpdateCommand;
import com.hunyuan.sa.base.module.support.dict.domain.form.DictAddForm;
import com.hunyuan.sa.base.module.support.dict.domain.form.DictDataAddForm;
import com.hunyuan.sa.base.module.support.dict.domain.form.DictDataUpdateForm;
import com.hunyuan.sa.base.module.support.dict.domain.form.DictQueryForm;
import com.hunyuan.sa.base.module.support.dict.domain.form.DictUpdateForm;
import com.hunyuan.sa.base.module.support.dict.domain.vo.DictDataVO;
import com.hunyuan.sa.base.module.support.dict.domain.vo.DictVO;
import com.hunyuan.sa.base.module.support.dict.service.DictService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 平台字典公开用例实现，负责隔离历史字典服务的数据契约。
 */
@Service
public class PlatformDictionaryApplicationService implements PlatformDictionaryFacade {

    @Resource
    private DictService dictService;

    @Override
    public PageResult<PlatformDictionarySummary> queryPage(PlatformDictionaryPageQuery query) {
        DictQueryForm legacyQuery = SmartBeanUtil.copy(query, DictQueryForm.class);
        PageResult<DictVO> legacyResult = dictService.queryPage(legacyQuery);

        PageResult<PlatformDictionarySummary> result = new PageResult<>();
        result.setPageNum(legacyResult.getPageNum());
        result.setPageSize(legacyResult.getPageSize());
        result.setTotal(legacyResult.getTotal());
        result.setPages(legacyResult.getPages());
        result.setEmptyFlag(legacyResult.getEmptyFlag());
        result.setList(SmartBeanUtil.copyList(legacyResult.getList(), PlatformDictionarySummary.class));
        return result;
    }

    @Override
    public List<PlatformDictionarySummary> getAllDictionaries() {
        return SmartBeanUtil.copyList(dictService.getAllDict(), PlatformDictionarySummary.class);
    }

    @Override
    public List<PlatformDictionaryItem> getAllItems() {
        return copyItems(dictService.getAll());
    }

    @Override
    public List<PlatformDictionaryItem> getItemsByDictionaryId(Long dictionaryId) {
        return copyItems(dictService.queryDictData(dictionaryId));
    }

    @Override
    public ResponseDTO<String> create(PlatformDictionaryCreateCommand command) {
        return dictService.add(SmartBeanUtil.copy(command, DictAddForm.class));
    }

    @Override
    public ResponseDTO<String> update(Long dictionaryId, PlatformDictionaryUpdateCommand command) {
        DictUpdateForm legacyCommand = SmartBeanUtil.copy(command, DictUpdateForm.class);
        legacyCommand.setDictId(dictionaryId);
        return dictService.update(legacyCommand);
    }

    @Override
    public ResponseDTO<String> toggleDisabled(Long dictionaryId) {
        return dictService.updateDisabled(dictionaryId);
    }

    @Override
    public ResponseDTO<String> batchDelete(List<Long> dictionaryIds) {
        return dictService.batchDelete(dictionaryIds);
    }

    @Override
    public ResponseDTO<String> delete(Long dictionaryId) {
        return dictService.delete(dictionaryId);
    }

    @Override
    public ResponseDTO<String> createItem(
            Long dictionaryId, PlatformDictionaryItemCreateCommand command) {
        DictDataAddForm legacyCommand = SmartBeanUtil.copy(command, DictDataAddForm.class);
        legacyCommand.setDictId(dictionaryId);
        return dictService.addDictData(legacyCommand);
    }

    @Override
    public ResponseDTO<String> updateItem(
            Long dictionaryId, Long dictionaryItemId, PlatformDictionaryItemUpdateCommand command) {
        DictDataUpdateForm legacyCommand = SmartBeanUtil.copy(command, DictDataUpdateForm.class);
        legacyCommand.setDictId(dictionaryId);
        legacyCommand.setDictDataId(dictionaryItemId);
        return dictService.updateDictData(legacyCommand);
    }

    @Override
    public ResponseDTO<String> toggleItemDisabled(Long dictionaryItemId) {
        return dictService.updateDictDataDisabled(dictionaryItemId);
    }

    @Override
    public ResponseDTO<String> batchDeleteItems(List<Long> dictionaryItemIds) {
        return dictService.batchDeleteDictData(dictionaryItemIds);
    }

    @Override
    public ResponseDTO<String> deleteItem(Long dictionaryItemId) {
        return dictService.deleteDictData(dictionaryItemId);
    }

    private List<PlatformDictionaryItem> copyItems(List<DictDataVO> items) {
        return SmartBeanUtil.copyList(items, PlatformDictionaryItem.class);
    }
}
