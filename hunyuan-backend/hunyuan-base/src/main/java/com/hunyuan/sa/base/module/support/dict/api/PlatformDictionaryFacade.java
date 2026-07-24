package com.hunyuan.sa.base.module.support.dict.api;

import com.hunyuan.sa.base.common.domain.PageResult;
import com.hunyuan.sa.base.common.domain.ResponseDTO;

import java.util.List;

/**
 * 平台字典公开边界。
 *
 * <p>业务模块和稳定 HTTP 接口只通过该边界读取字典，不直接依赖历史服务与数据对象。</p>
 */
public interface PlatformDictionaryFacade {

    PageResult<PlatformDictionarySummary> queryPage(PlatformDictionaryPageQuery query);

    List<PlatformDictionarySummary> getAllDictionaries();

    List<PlatformDictionaryItem> getAllItems();

    List<PlatformDictionaryItem> getItemsByDictionaryId(Long dictionaryId);

    ResponseDTO<String> create(PlatformDictionaryCreateCommand command);

    ResponseDTO<String> update(Long dictionaryId, PlatformDictionaryUpdateCommand command);

    ResponseDTO<String> toggleDisabled(Long dictionaryId);

    ResponseDTO<String> batchDelete(List<Long> dictionaryIds);

    ResponseDTO<String> delete(Long dictionaryId);

    ResponseDTO<String> createItem(Long dictionaryId, PlatformDictionaryItemCreateCommand command);

    ResponseDTO<String> updateItem(
            Long dictionaryId, Long dictionaryItemId, PlatformDictionaryItemUpdateCommand command);

    ResponseDTO<String> toggleItemDisabled(Long dictionaryItemId);

    ResponseDTO<String> batchDeleteItems(List<Long> dictionaryItemIds);

    ResponseDTO<String> deleteItem(Long dictionaryItemId);
}
