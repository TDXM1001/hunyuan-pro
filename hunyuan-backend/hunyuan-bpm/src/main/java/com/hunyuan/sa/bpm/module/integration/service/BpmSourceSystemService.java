package com.hunyuan.sa.bpm.module.integration.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.module.integration.dao.*;
import com.hunyuan.sa.bpm.module.integration.domain.entity.*;
import com.hunyuan.sa.bpm.module.integration.domain.model.BpmSourceApplicationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.nio.charset.StandardCharsets; import java.security.MessageDigest; import java.time.*; import javax.crypto.Mac; import javax.crypto.spec.SecretKeySpec;

@Service
public class BpmSourceSystemService {
    private final BpmSourceSystemVersionDao systemDao; private final BpmSourceApplicationDao appDao; private final BpmExternalRequestNonceDao nonceDao; private final Clock clock;
    @Autowired public BpmSourceSystemService(BpmSourceSystemVersionDao s,BpmSourceApplicationDao a,BpmExternalRequestNonceDao n){this(s,a,n,Clock.systemUTC());}
    public BpmSourceSystemService(BpmSourceSystemVersionDao s,BpmSourceApplicationDao a,BpmExternalRequestNonceDao n,Clock c){systemDao=s;appDao=a;nonceDao=n;clock=c;}
    public BpmSourceApplicationPrincipal authenticate(String appCode,String timestamp,String nonce,String signature,String method,String target,String body){
        BpmSourceApplicationEntity app=appDao.selectOne(Wrappers.<BpmSourceApplicationEntity>lambdaQuery().eq(BpmSourceApplicationEntity::getApplicationCode,appCode).eq(BpmSourceApplicationEntity::getStatus,"ACTIVE").last("LIMIT 1"));
        if(app==null) throw new SecurityException("外部应用未授权");
        LocalDateTime current=LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC);
        if(app.getExpiresAt()!=null&&!app.getExpiresAt().isAfter(current))throw new SecurityException("外部应用凭据已过期");
        BpmSourceSystemVersionEntity system=systemDao.selectById(app.getSourceSystemVersionId());
        if(system==null||!"PUBLISHED".equals(system.getStatus())||!java.util.Objects.equals(system.getSourceSystemCode(),app.getSourceSystemCode()))throw new SecurityException("来源系统版本未发布或引用失效");
        Instant now=clock.instant(); long epoch;
        try{epoch=Long.parseLong(timestamp);}catch(Exception e){throw new SecurityException("请求时间戳无效");}
        if(Math.abs(now.getEpochSecond()-epoch)>300) throw new SecurityException("请求已过期");
        if(nonceDao.selectCount(Wrappers.<BpmExternalRequestNonceEntity>lambdaQuery().eq(BpmExternalRequestNonceEntity::getApplicationId,app.getApplicationId()).eq(BpmExternalRequestNonceEntity::getNonceValue,nonce))>0) throw new SecurityException("请求 nonce 已使用");
        String secret=resolveSecret(app.getSecretRef()); String expected=hmac(secret,timestamp+"\n"+nonce+"\n"+method+"\n"+target+"\n"+(body==null?"":body));
        if(!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),String.valueOf(signature).getBytes(StandardCharsets.UTF_8))) throw new SecurityException("请求签名无效");
        BpmExternalRequestNonceEntity record=new BpmExternalRequestNonceEntity(); record.setApplicationId(app.getApplicationId());record.setNonceValue(nonce);record.setExpiresAt(LocalDateTime.ofInstant(now.plusSeconds(600),ZoneOffset.UTC));nonceDao.insert(record);
        return new BpmSourceApplicationPrincipal(app.getApplicationId(),app.getSourceSystemCode(),app.getApplicationCode(),app.getScopes(),app.getStatus());
    }
    private String resolveSecret(String ref){if(ref==null||!ref.startsWith("env:"))throw new SecurityException("应用密钥引用无效");String value=System.getenv(ref.substring(4));if(value==null||value.isBlank())throw new SecurityException("应用密钥不可用");return value;}
    private String hmac(String secret,String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("签名计算失败",e);}}
}
