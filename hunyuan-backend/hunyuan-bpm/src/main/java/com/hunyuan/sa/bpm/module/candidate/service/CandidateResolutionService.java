package com.hunyuan.sa.bpm.module.candidate.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.candidate.domain.model.CandidateDiagnostic;
import com.hunyuan.sa.bpm.module.candidate.domain.model.CandidateResolutionContext;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyPublicationLease;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ResolvedCandidateMember;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ResolvedCandidateSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 按定义版本冻结的候选策略解析成员，不读取可变表单数据。
 */
@Service
public class CandidateResolutionService {

    private final BpmOrgIdentityGateway identityGateway;

    public CandidateResolutionService(BpmOrgIdentityGateway identityGateway) {
        this.identityGateway = identityGateway;
    }

    public ResolvedCandidateSnapshot resolve(PolicyPublicationLease policy, CandidateResolutionContext context) {
        if (policy == null || policy.canonicalPayload() == null) {
            throw new IllegalArgumentException("候选策略冻结内容不能为空");
        }
        if (context == null) {
            throw new IllegalArgumentException("候选解析上下文不能为空");
        }
        JSONObject payload = JSON.parseObject(policy.canonicalPayload());
        String resolverType = payload.getString("resolverType");
        JSONObject parameters = payload.getJSONObject("resolverParameters");
        return switch (resolverType == null ? "" : resolverType.toUpperCase()) {
            case "EMPLOYEE" -> resolveEmployees(readEmployeeIds(parameters));
            case "ROLE" -> resolveRole(requiredId(parameters, "roleId", "ROLE 候选策略缺少有效 roleId"));
            case "DEPARTMENT_MANAGER" -> resolveDepartmentManager(
                    requiredId(parameters, "departmentId", "DEPARTMENT_MANAGER 候选策略缺少有效 departmentId")
            );
            case "START_EMPLOYEE" -> resolveStartEmployee(context);
            case "START_DEPARTMENT_MANAGER" -> resolveStartDepartmentManager(context);
            case "ROUTING_FACT_EMPLOYEE" -> resolveRoutingFactEmployee(parameters, context);
            case "POST" -> resolvePost(requiredId(parameters, "positionId", "POST 候选策略缺少有效 positionId"));
            case "USER_GROUP", "MANAGEMENT_CHAIN" -> throw new IllegalStateException(
                    "组织来源 " + resolverType + " 尚未由组织域提供受控主数据，已拒绝解析"
            );
            default -> throw new IllegalArgumentException("当前候选解析器类型尚未注册：" + resolverType);
        };
    }

    private ResolvedCandidateSnapshot resolveRole(Long roleId) {
        List<Long> employeeIds = identityGateway.listEmployeeIdsByRoleId(roleId);
        return resolveEmployees(employeeIds);
    }

    private ResolvedCandidateSnapshot resolveDepartmentManager(Long departmentId) {
        Long managerId = identityGateway.resolveDepartmentManagerEmployeeId(departmentId);
        return resolveEmployees(managerId == null ? List.of() : List.of(managerId));
    }

    private ResolvedCandidateSnapshot resolvePost(Long positionId) {
        return resolveEmployees(identityGateway.listActiveEmployeeIdsByPositionId(positionId));
    }

    private ResolvedCandidateSnapshot resolveStartEmployee(CandidateResolutionContext context) {
        BpmEmployeeSnapshot startEmployee = context.startEmployee();
        if (startEmployee == null || startEmployee.employeeId() == null) {
            throw new IllegalArgumentException("候选策略缺少服务端确认的发起人员工");
        }
        return resolveEmployees(List.of(startEmployee.employeeId()));
    }

    private ResolvedCandidateSnapshot resolveStartDepartmentManager(CandidateResolutionContext context) {
        BpmEmployeeSnapshot startEmployee = context.startEmployee();
        if (startEmployee == null || startEmployee.departmentId() == null) {
            throw new IllegalArgumentException("候选策略缺少服务端确认的发起人部门");
        }
        return resolveDepartmentManager(startEmployee.departmentId());
    }

    private ResolvedCandidateSnapshot resolveRoutingFactEmployee(
            JSONObject parameters,
            CandidateResolutionContext context
    ) {
        String factKey = parameters == null ? null : parameters.getString("factKey");
        if (context.routingFactView() == null) {
            throw new IllegalArgumentException("候选策略需要 M3 路由事实");
        }
        return resolveEmployees(List.of(context.routingFactView().requireEmployeeFact(factKey)));
    }

    private ResolvedCandidateSnapshot resolveEmployees(List<Long> employeeIds) {
        Set<Long> sortedEmployeeIds = new TreeSet<>();
        if (employeeIds != null) {
            employeeIds.stream().filter(id -> id != null && id > 0).forEach(sortedEmployeeIds::add);
        }
        List<ResolvedCandidateMember> members = new ArrayList<>();
        List<CandidateDiagnostic> diagnostics = new ArrayList<>();
        for (Long employeeId : sortedEmployeeIds) {
            try {
                BpmEmployeeSnapshot employee = identityGateway.requireEmployee(employeeId);
                members.add(new ResolvedCandidateMember(
                        employeeId,
                        employee.employeeId(),
                        employee.actualName(),
                        employee.departmentId(),
                        employee.departmentName()
                ));
            } catch (IllegalArgumentException ex) {
                diagnostics.add(new CandidateDiagnostic("IDENTITY_UNAVAILABLE", ex.getMessage(), employeeId));
            }
        }
        return new ResolvedCandidateSnapshot(members, diagnostics);
    }

    private List<Long> readEmployeeIds(JSONObject parameters) {
        JSONArray employeeIds = parameters == null ? null : parameters.getJSONArray("employeeIds");
        if (employeeIds == null || employeeIds.isEmpty()) {
            throw new IllegalArgumentException("EMPLOYEE 候选策略缺少有效 employeeIds");
        }
        return employeeIds.toJavaList(Long.class);
    }

    private Long requiredId(JSONObject parameters, String fieldName, String errorMessage) {
        Long value = parameters == null ? null : parameters.getLong(fieldName);
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }
}
