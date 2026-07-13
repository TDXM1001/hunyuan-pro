package com.hunyuan.sa.bpm.integration;

import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;
import com.hunyuan.sa.bpm.module.integration.dao.BpmCallbackRecordDao;
import com.hunyuan.sa.bpm.module.integration.dao.BpmEventSubscriptionVersionDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCallbackRecordEntity;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmEventSubscriptionVersionEntity;
import com.hunyuan.sa.bpm.module.integration.service.BpmEventSubscriptionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class BpmM6EventSubscriptionServiceTest {
    @Test
    void resultEventMustFanOutToEveryFrozenSubscriptionOnce() {
        BpmEventSubscriptionVersionDao subscriptionDao=Mockito.mock(BpmEventSubscriptionVersionDao.class);
        BpmCallbackRecordDao callbackDao=Mockito.mock(BpmCallbackRecordDao.class);
        BpmEventSubscriptionVersionEntity first=subscription(11L,"purchase-result-a");
        BpmEventSubscriptionVersionEntity second=subscription(12L,"purchase-result-b");
        Mockito.when(subscriptionDao.selectList(Mockito.any())).thenReturn(List.of(first,second));
        Mockito.when(callbackDao.selectCount(Mockito.any())).thenReturn(0L);
        com.hunyuan.sa.bpm.module.integration.service.BpmExternalPublicReferenceService publicReferences=Mockito.mock(com.hunyuan.sa.bpm.module.integration.service.BpmExternalPublicReferenceService.class);
        Mockito.when(publicReferences.getOrCreate("PURCHASE","INSTANCE",88L)).thenReturn("BP-opaque-88");
        BpmEventSubscriptionService service=new BpmEventSubscriptionService(subscriptionDao,callbackDao,publicReferences);
        BpmBusinessResultEvent event=new BpmBusinessResultEvent(); event.setEventId("event-1");event.setInstanceId(88L);event.setSourceSystemCode("PURCHASE");event.setBusinessType("purchase");event.setBusinessKey("PO-1001");event.setResultState(1);

        assertThat(service.enqueue(event)).isEqualTo(2);
        ArgumentCaptor<BpmCallbackRecordEntity> records=ArgumentCaptor.forClass(BpmCallbackRecordEntity.class);
        Mockito.verify(callbackDao,Mockito.times(2)).insert(records.capture());
        assertThat(records.getAllValues()).extracting(BpmCallbackRecordEntity::getSubscriptionVersionId).containsExactly(11L,12L);
        assertThat(records.getAllValues()).allSatisfy(record -> assertThat(record.getRetryPolicyJson()).contains("maxAttempts"));
        assertThat(records.getAllValues()).allSatisfy(record -> assertThat(record.getRequestPayloadJson()).contains("BP-opaque-88").doesNotContain("\"instanceId\""));
        Mockito.verify(subscriptionDao).selectList(Mockito.any());
    }
    @Test
    void resultEventMustCarrySourceSystemIsolationFact() {
        BpmBusinessResultEvent event=new BpmBusinessResultEvent();event.setSourceSystemCode("PURCHASE");
        assertThat(event.getSourceSystemCode()).isEqualTo("PURCHASE");
        assertThat(com.alibaba.fastjson.JSON.toJSONString(event)).contains("sourceSystemCode");
    }
    private BpmEventSubscriptionVersionEntity subscription(Long id,String key){BpmEventSubscriptionVersionEntity e=new BpmEventSubscriptionVersionEntity();e.setSubscriptionVersionId(id);e.setSubscriptionKey(key);e.setSubscriptionVersion(1);e.setSourceSystemCode("PURCHASE");e.setBusinessType("purchase");e.setEventType("PROCESS_RESULT");e.setConnectorKey("purchase-callback");e.setEndpointOperation("result");e.setSigningSecretRef("env:PURCHASE_CALLBACK_SECRET");e.setRetryPolicyJson("{\"maxAttempts\":3}");e.setStatus("PUBLISHED");return e;}
}
