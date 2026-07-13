package com.hunyuan.sa.bpm.module.integration.service;

import com.alibaba.fastjson.JSON; import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.api.business.domain.BpmBusinessResultEvent;
import com.hunyuan.sa.bpm.module.integration.dao.*; import com.hunyuan.sa.bpm.module.integration.domain.entity.*;
import org.springframework.stereotype.Service; import java.time.LocalDateTime; import java.util.List;

@Service
public class BpmEventSubscriptionService {
 private final BpmEventSubscriptionVersionDao subscriptions; private final BpmCallbackRecordDao callbacks; private final BpmExternalPublicReferenceService publicReferences;
 public BpmEventSubscriptionService(BpmEventSubscriptionVersionDao s,BpmCallbackRecordDao c,BpmExternalPublicReferenceService p){subscriptions=s;callbacks=c;publicReferences=p;}
 public int enqueue(BpmBusinessResultEvent event){
  List<BpmEventSubscriptionVersionEntity> active=subscriptions.selectList(Wrappers.<BpmEventSubscriptionVersionEntity>lambdaQuery()
   .eq(BpmEventSubscriptionVersionEntity::getSourceSystemCode,event.getSourceSystemCode()).eq(BpmEventSubscriptionVersionEntity::getBusinessType,event.getBusinessType()).eq(BpmEventSubscriptionVersionEntity::getEventType,"PROCESS_RESULT").eq(BpmEventSubscriptionVersionEntity::getStatus,"PUBLISHED"));
  int inserted=0;
  for(BpmEventSubscriptionVersionEntity sub:active){
   long count=callbacks.selectCount(Wrappers.<BpmCallbackRecordEntity>lambdaQuery().eq(BpmCallbackRecordEntity::getEventId,event.getEventId()).eq(BpmCallbackRecordEntity::getSubscriptionVersionId,sub.getSubscriptionVersionId()));
   if(count>0)continue;
   com.alibaba.fastjson.JSONObject payload=(com.alibaba.fastjson.JSONObject)JSON.toJSON(event);payload.remove("instanceId");payload.put("instanceNo",publicReferences.getOrCreate(event.getSourceSystemCode(),"INSTANCE",event.getInstanceId()));
   BpmCallbackRecordEntity r=new BpmCallbackRecordEntity();r.setEventId(event.getEventId());r.setSubscriptionVersionId(sub.getSubscriptionVersionId());r.setConnectorKey(sub.getConnectorKey());r.setConnectorVersion(sub.getConnectorVersion());r.setEndpointOperation(sub.getEndpointOperation());r.setSigningSecretRef(sub.getSigningSecretRef());r.setRetryPolicyJson(sub.getRetryPolicyJson());r.setInstanceId(event.getInstanceId());r.setBusinessType(event.getBusinessType());r.setBusinessId(event.getBusinessId());r.setBusinessKey(event.getBusinessKey());r.setCallbackStatus(0);r.setRetryCount(0);r.setRequestPayloadJson(payload.toJSONString());r.setUpdateTime(LocalDateTime.now());callbacks.insert(r);inserted++;
  }
  return inserted;
 }
}
