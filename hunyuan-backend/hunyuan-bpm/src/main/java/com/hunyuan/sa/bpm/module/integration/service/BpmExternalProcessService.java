package com.hunyuan.sa.bpm.module.integration.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hunyuan.sa.bpm.api.identity.BpmActorScope;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.GenericApplicationSubmitCommand;
import com.hunyuan.sa.bpm.module.approvaldata.domain.model.GenericApplicationSubmitResult;
import com.hunyuan.sa.bpm.module.approvaldata.service.BpmGenericApplicationService;
import com.hunyuan.sa.bpm.module.businesscontract.service.BpmBusinessContractCatalogService;
import com.hunyuan.sa.bpm.module.integration.domain.command.BpmExternalStartCommand;
import com.hunyuan.sa.bpm.module.integration.domain.command.BpmExternalTaskActionCommand;
import com.hunyuan.sa.bpm.module.integration.domain.model.BpmExternalTaskView;
import com.hunyuan.sa.bpm.module.integration.domain.entity.BpmCommandRecordEntity;
import com.hunyuan.sa.bpm.module.integration.domain.model.BpmProcessBindingMatch;
import com.hunyuan.sa.bpm.module.integration.domain.model.BpmSourceApplicationPrincipal;
import org.springframework.stereotype.Service;
import com.hunyuan.sa.bpm.module.runtime.service.BpmTaskService;
import com.hunyuan.sa.bpm.module.runtime.dao.BpmTaskDao;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskApproveForm;
import com.hunyuan.sa.bpm.module.runtime.domain.form.BpmTaskRejectForm;
import java.util.Map; import java.util.List;

@Service
public class BpmExternalProcessService {
    private final BpmExternalStartCommandStore commandStore; private final BpmExternalEmployeeMappingService mappings;
    private final BpmProcessBindingService bindings; private final BpmGenericApplicationService applications;
    private final BpmBusinessContractCatalogService contracts;
    private final BpmTaskService tasks;
    private final BpmExternalPublicReferenceService publicReferences;
    private final BpmTaskDao taskDao;
    public BpmExternalProcessService(BpmExternalStartCommandStore c,BpmExternalEmployeeMappingService m,BpmProcessBindingService b,BpmGenericApplicationService a,BpmBusinessContractCatalogService contracts,BpmTaskService tasks,BpmExternalPublicReferenceService publicReferences,BpmTaskDao taskDao){commandStore=c;mappings=m;bindings=b;applications=a;this.contracts=contracts;this.tasks=tasks;this.publicReferences=publicReferences;this.taskDao=taskDao;}
    public String start(BpmSourceApplicationPrincipal principal,BpmExternalStartCommand command){
        require(principal,"process:start"); validate(command);
        String requestJson=JSON.toJSONString(command);String key="EXTERNAL_START:"+principal.applicationId()+":"+command.requestId();
        BpmExternalStartCommandStore.Claim claim=commandStore.loadOrCreate(key,command.businessType(),requestJson);
        BpmCommandRecordEntity record=claim.command();
        if(!java.util.Objects.equals(record.getRequestPayloadJson(),requestJson))throw new IllegalStateException("requestId 已用于不同请求");
        if(record.getCommandStatus()!=null&&record.getCommandStatus()==1&&record.getInstanceId()!=null)return publicReferences.getOrCreate(principal.sourceSystemCode(),"INSTANCE",record.getInstanceId());
        if(!claim.owner())throw new IllegalStateException(record.getCommandStatus()!=null&&record.getCommandStatus()==2?"外部发起请求已失败":"外部发起请求正在处理");
        try{
            Long employeeId=mappings.requireEmployee(principal,command.externalEmployeeId());
            BpmProcessBindingMatch binding=bindings.resolve(command.businessType(),null,command.scenario(),parseFacts(command.formDataJson()));
            var contract=contracts.freezeForPublication(command.contractKey(),command.contractVersion());
            GenericApplicationSubmitResult result=BpmActorScope.runAs(employeeId,()->applications.submit(new GenericApplicationSubmitCommand(binding.graphDefinitionVersionId(),contract.contractKey(),contract.contractVersion(),principal.sourceSystemCode(),command.businessType(),command.businessKey(),command.title(),null,command.formDataJson(),"[]","[]",command.formDataJson(),command.formDataJson())));
            commandStore.markSucceeded(record.getCommandRecordId(),result.instanceId());return publicReferences.getOrCreate(principal.sourceSystemCode(),"INSTANCE",result.instanceId());
        }catch(RuntimeException ex){commandStore.markFailed(record.getCommandRecordId(),ex.getMessage());throw ex;}
    }
    private void require(BpmSourceApplicationPrincipal p,String scope){if(p==null||!p.hasScope(scope))throw new SecurityException("外部应用缺少 scope: "+scope);}
    private void validate(BpmExternalStartCommand c){if(c==null||c.requestId()==null||c.requestId().isBlank()||c.contractKey()==null||c.contractVersion()==null||c.businessType()==null||c.businessKey()==null||c.externalEmployeeId()==null)throw new IllegalArgumentException("外部发起参数不完整");}
    @SuppressWarnings("unchecked") private Map<String,Object> parseFacts(String json){return json==null?Map.of():JSON.parseObject(json,Map.class);}
    public BpmExternalTaskView getTask(BpmSourceApplicationPrincipal principal,String externalEmployeeId,String taskNo){
        require(principal,"task:read");Long employeeId=mappings.requireEmployee(principal,externalEmployeeId);Long taskId=publicReferences.resolve(principal.sourceSystemCode(),"TASK",taskNo);
        var response=BpmActorScope.runAs(employeeId,()->tasks.getMyDetail(taskId));
        if(!Boolean.TRUE.equals(response.getOk())||response.getData()==null)throw new SecurityException(response.getMsg());var v=response.getData();
        var subject=v.getApprovalSubjectContext();var externalSubject=subject==null?null:new BpmExternalTaskView.ApprovalSubjectView(subject.getViewState(),subject.getDiagnosticMessage(),subject.getTitle(),subject.getSummary(),subject.getFieldsJson(),subject.getLineItemsJson(),subject.getAttachmentsJson(),subject.getWorkingDataJson(),subject.getWorkingDataVersion(),subject.getFieldPermissions());
        return new BpmExternalTaskView(taskNo,publicReferences.getOrCreate(principal.sourceSystemCode(),"INSTANCE",v.getInstanceId()),v.getInstanceTitle(),v.getTaskName(),v.getTaskVersion(),v.getAvailableActions(),v.getFormContext(),externalSubject);
    }
    public List<BpmExternalTaskView> listTasks(BpmSourceApplicationPrincipal principal,String externalEmployeeId,String instanceNo){
        require(principal,"task:read");Long employeeId=mappings.requireEmployee(principal,externalEmployeeId);Long instanceId=publicReferences.resolve(principal.sourceSystemCode(),"INSTANCE",instanceNo);
        return taskDao.selectList(Wrappers.<com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity>lambdaQuery().eq(com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity::getInstanceId,instanceId).eq(com.hunyuan.sa.bpm.module.runtime.domain.entity.BpmTaskEntity::getTaskState,1)).stream().map(task->{String taskNo=publicReferences.getOrCreate(principal.sourceSystemCode(),"TASK",task.getTaskId());return getTask(principal,externalEmployeeId,taskNo);}).toList();
    }
    public void act(BpmSourceApplicationPrincipal principal,String taskNo,BpmExternalTaskActionCommand command){
        require(principal,"task:action");Long employeeId=mappings.requireEmployee(principal,command.externalEmployeeId());Long taskId=publicReferences.resolve(principal.sourceSystemCode(),"TASK",taskNo);
        var response=BpmActorScope.runAs(employeeId,()->{
            if("APPROVE".equalsIgnoreCase(command.action())){BpmTaskApproveForm f=new BpmTaskApproveForm();f.setTaskId(taskId);f.setTaskVersion(command.taskVersion());f.setRequestId(command.requestId());f.setCommentText(command.commentText());f.setFormDataVersion(command.formDataVersion());f.setFormDataPatchJson(command.formDataPatchJson());return tasks.approve(f);}
            if("REJECT".equalsIgnoreCase(command.action())){BpmTaskRejectForm f=new BpmTaskRejectForm();f.setTaskId(taskId);f.setTaskVersion(command.taskVersion());f.setRequestId(command.requestId());f.setCommentText(command.commentText());return tasks.reject(f);}
            throw new IllegalArgumentException("外部任务动作仅支持 APPROVE/REJECT");
        });if(!Boolean.TRUE.equals(response.getOk()))throw new IllegalStateException(response.getMsg());
    }
}
