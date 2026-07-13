package com.hunyuan.sa.bpm.module.integration.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.module.integration.dao.BpmProcessBindingVersionDao;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmProcessBindingVersionEntity;
import com.hunyuan.sa.bpm.module.integration.domain.model.BpmProcessBindingMatch;
import org.springframework.stereotype.Service;
import java.util.List; import java.util.Map;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.time.LocalDateTime;

@Service
public class BpmProcessBindingService {
    private final BpmProcessBindingVersionDao dao;
    public BpmProcessBindingService(BpmProcessBindingVersionDao dao){this.dao=dao;}
    public BpmProcessBindingMatch resolve(String businessType, Long organizationId, String scenario, Map<String,Object> facts){
        List<BpmProcessBindingVersionEntity> matches=dao.selectList(Wrappers.<BpmProcessBindingVersionEntity>lambdaQuery()
                .eq(BpmProcessBindingVersionEntity::getBusinessType,businessType)
                .eq(BpmProcessBindingVersionEntity::getStatus,"PUBLISHED")
                .and(q->q.isNull(BpmProcessBindingVersionEntity::getOrganizationId).or().eq(BpmProcessBindingVersionEntity::getOrganizationId,organizationId))
                .and(q->q.isNull(BpmProcessBindingVersionEntity::getScenario).or().eq(BpmProcessBindingVersionEntity::getScenario,scenario))
                .orderByDesc(BpmProcessBindingVersionEntity::getPriority));
        LocalDateTime now=LocalDateTime.now();
        matches=matches==null?List.of():matches.stream().filter(it->(it.getEffectiveFrom()==null||!it.getEffectiveFrom().isAfter(now))&&(it.getEffectiveUntil()==null||it.getEffectiveUntil().isAfter(now))).filter(it->matches(it.getConditionJson(),facts)).toList();
        if(matches.isEmpty()) throw new IllegalStateException("没有命中的流程绑定");
        int top=matches.get(0).getPriority()==null?0:matches.get(0).getPriority();
        List<BpmProcessBindingVersionEntity> topMatches=matches.stream().filter(it->(it.getPriority()==null?0:it.getPriority())==top).toList();
        if(topMatches.size()!=1) throw new IllegalStateException("流程绑定冲突，必须唯一命中");
        BpmProcessBindingVersionEntity e=topMatches.get(0);
        return new BpmProcessBindingMatch(e.getBindingVersionId(),e.getGraphDefinitionVersionId(),e.getBindingKey(),e.getBindingVersion());
    }
    private boolean matches(String conditionJson,Map<String,Object> facts){if(conditionJson==null||conditionJson.isBlank()||"{}".equals(conditionJson.trim()))return true;JSONObject root=JSON.parseObject(conditionJson);JSONArray rules=root.getJSONArray("all");if(rules==null)throw new IllegalStateException("流程绑定条件必须使用 all 类型化规则");for(int i=0;i<rules.size();i++){JSONObject r=rules.getJSONObject(i);if(!compare(facts.get(r.getString("fact")),r.getString("operator"),r.get("value")))return false;}return true;}
    private boolean compare(Object actual,String operator,Object expected){if(operator==null)throw new IllegalStateException("流程绑定条件缺少 operator");return switch(operator){case "EQ"->java.util.Objects.equals(normalize(actual),normalize(expected));case "NE"->!java.util.Objects.equals(normalize(actual),normalize(expected));case "GT"->number(actual)>number(expected);case "GTE"->number(actual)>=number(expected);case "LT"->number(actual)<number(expected);case "LTE"->number(actual)<=number(expected);case "IN"->expected instanceof java.util.Collection<?> c&&c.stream().map(this::normalize).anyMatch(v->java.util.Objects.equals(v,normalize(actual)));default->throw new IllegalStateException("不支持的流程绑定条件操作符: "+operator);};}
    private Object normalize(Object value){return value instanceof Number?new java.math.BigDecimal(value.toString()).stripTrailingZeros():value;}
    private double number(Object value){if(value==null)throw new IllegalStateException("流程绑定数值条件缺少事实");return Double.parseDouble(value.toString());}
}
