package com.hunyuan.sa.bpm.module.candidate.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hunyuan.sa.bpm.api.identity.BpmEmployeeSnapshot;
import com.hunyuan.sa.bpm.api.identity.BpmOrgIdentityGateway;
import com.hunyuan.sa.bpm.module.candidate.domain.model.CandidateDiagnostic;
import com.hunyuan.sa.bpm.module.candidate.domain.model.CandidateAutomaticOutcome;
import com.hunyuan.sa.bpm.module.candidate.domain.model.CandidateResolutionContext;
import com.hunyuan.sa.bpm.module.candidate.domain.model.PolicyPublicationLease;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ResolvedCandidateMember;
import com.hunyuan.sa.bpm.module.candidate.domain.model.ResolvedCandidateSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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
        ResolvedCandidateSnapshot resolved = switch (resolverType == null ? "" : resolverType.toUpperCase()) {
            case "EMPLOYEE" -> resolveEmployees(readEmployeeIds(parameters));
            case "ROLE" -> resolveRole(requiredId(parameters, "roleId", "ROLE 候选策略缺少有效 roleId"));
            case "DEPARTMENT_MANAGER" -> resolveDepartmentManager(
                    requiredId(parameters, "departmentId", "DEPARTMENT_MANAGER 候选策略缺少有效 departmentId")
            );
            case "START_EMPLOYEE" -> resolveStartEmployee(context);
            case "START_DEPARTMENT_MANAGER" -> resolveStartDepartmentManager(context);
            case "ROUTING_FACT_EMPLOYEE" -> resolveRoutingFactEmployee(parameters, context);
            case "POST" -> resolvePost(requiredId(parameters, "positionId", "POST 候选策略缺少有效 positionId"));
            case "USER_GROUP" -> resolveEmployees(identityGateway.listActiveEmployeeIdsByUserGroupId(
                    requiredId(parameters, "userGroupId", "USER_GROUP 候选策略缺少有效 userGroupId")
            ));
            case "MANAGEMENT_CHAIN" -> resolveManagementChain(parameters, context);
            default -> throw new IllegalArgumentException("当前候选解析器类型尚未注册：" + resolverType);
        };
        return applyCandidatePolicies(payload, context, resolved);
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

    private ResolvedCandidateSnapshot resolveManagementChain(
            JSONObject parameters,
            CandidateResolutionContext context
    ) {
        String chainType = parameters == null ? null : parameters.getString("chainType");
        Integer maxDepth = parameters == null ? null : parameters.getInteger("maxDepth");
        JSONObject seed = parameters == null ? null : parameters.getJSONObject("seedIdentityReference");
        if (chainType == null || maxDepth == null || maxDepth < 1 || maxDepth > 20 || seed == null) {
            throw new IllegalArgumentException("MANAGEMENT_CHAIN 参数不完整");
        }
        List<Long> employeeIds = switch (chainType) {
            case "DEPARTMENT_MANAGER_CHAIN" -> identityGateway.listDepartmentManagerChain(
                    resolveSeedDepartmentId(seed, context), maxDepth
            );
            case "EMPLOYEE_REPORTING_CHAIN" -> identityGateway.listEmployeeReportingManagerChain(
                    resolveSeedEmployeeId(seed, context), maxDepth
            );
            default -> throw new IllegalArgumentException("MANAGEMENT_CHAIN chainType 不合法：" + chainType);
        };
        return resolveEmployees(employeeIds, true);
    }

    private Long resolveSeedDepartmentId(JSONObject seed, CandidateResolutionContext context) {
        String kind = seed.getString("kind");
        return switch (kind == null ? "" : kind) {
            case "DEPARTMENT" -> requiredId(seed, "stableId", "主管链缺少部门种子");
            case "EMPLOYEE" -> identityGateway.requireEmployee(
                    requiredId(seed, "stableId", "主管链缺少员工种子")
            ).departmentId();
            case "START_EMPLOYEE" -> context.startEmployee() == null ? null : context.startEmployee().departmentId();
            case "ROUTING_FACT_EMPLOYEE" -> identityGateway.requireEmployee(
                    requireRoutingFactEmployee(seed, context)
            ).departmentId();
            default -> throw new IllegalArgumentException("主管链部门种子类型不合法：" + kind);
        };
    }

    private Long resolveSeedEmployeeId(JSONObject seed, CandidateResolutionContext context) {
        String kind = seed.getString("kind");
        Long employeeId = switch (kind == null ? "" : kind) {
            case "EMPLOYEE" -> requiredId(seed, "stableId", "主管链缺少员工种子");
            case "START_EMPLOYEE" -> context.startEmployee() == null ? null : context.startEmployee().employeeId();
            case "ROUTING_FACT_EMPLOYEE" -> requireRoutingFactEmployee(seed, context);
            default -> throw new IllegalArgumentException("员工汇报链种子类型不合法：" + kind);
        };
        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException("员工汇报链种子无效");
        }
        return employeeId;
    }

    private Long requireRoutingFactEmployee(JSONObject seed, CandidateResolutionContext context) {
        if (context.routingFactView() == null) {
            throw new IllegalArgumentException("主管链需要 M3 路由事实");
        }
        return context.routingFactView().requireEmployeeFact(seed.getString("factKey"));
    }

    private ResolvedCandidateSnapshot applyCandidatePolicies(
            JSONObject payload,
            CandidateResolutionContext context,
            ResolvedCandidateSnapshot resolved
    ) {
        List<ResolvedCandidateMember> members = new ArrayList<>(resolved.members());
        List<CandidateDiagnostic> diagnostics = new ArrayList<>(resolved.diagnostics());
        Long starterId = context.startEmployee() == null ? null : context.startEmployee().employeeId();
        String selfPolicy = defaultText(payload.getString("selfApprovalPolicy"), "BLOCK");
        boolean containsStarter = starterId != null
                && members.stream().anyMatch(member -> starterId.equals(member.employeeId()));
        if (containsStarter) {
            switch (selfPolicy) {
                case "ALLOW" -> diagnostics.add(new CandidateDiagnostic(
                        "SELF_APPROVAL_ALLOWED", "冻结策略允许发起人自审", starterId
                ));
                case "SKIP_SELF" -> {
                    members.removeIf(member -> starterId.equals(member.employeeId()));
                    diagnostics.add(new CandidateDiagnostic(
                            "SELF_APPROVAL_SKIPPED", "已按冻结策略移除发起人", starterId
                    ));
                }
                case "ASSIGN_DEPARTMENT_MANAGER" -> {
                    members.removeIf(member -> starterId.equals(member.employeeId()));
                    Long departmentId = context.startEmployee() == null ? null : context.startEmployee().departmentId();
                    Long managerId = departmentId == null ? null : identityGateway.resolveDepartmentManagerEmployeeId(departmentId);
                    if (managerId == null || managerId.equals(starterId)) {
                        throw new IllegalStateException("自审改派部门主管失败关闭");
                    }
                    members.addAll(resolveEmployees(List.of(managerId)).members());
                }
                case "BLOCK" -> throw new IllegalStateException("冻结候选命中发起人，禁止自审");
                default -> throw new IllegalArgumentException("自审策略不合法：" + selfPolicy);
            }
        }
        if (!members.isEmpty()) {
            return new ResolvedCandidateSnapshot(members, diagnostics);
        }
        return handleEmptyCandidates(payload, context, diagnostics);
    }

    private ResolvedCandidateSnapshot handleEmptyCandidates(
            JSONObject payload,
            CandidateResolutionContext context,
            List<CandidateDiagnostic> diagnostics
    ) {
        String emptyPolicy = defaultText(payload.getString("emptyCandidatePolicy"), "BLOCK");
        return switch (emptyPolicy) {
            case "AUTO_APPROVE" -> new ResolvedCandidateSnapshot(
                    List.of(), appendDiagnostic(diagnostics, "EMPTY_AUTO_APPROVE", "空候选自动通过"),
                    CandidateAutomaticOutcome.AUTO_APPROVE
            );
            case "AUTO_REJECT" -> new ResolvedCandidateSnapshot(
                    List.of(), appendDiagnostic(diagnostics, "EMPTY_AUTO_REJECT", "空候选自动拒绝"),
                    CandidateAutomaticOutcome.AUTO_REJECT
            );
            case "ASSIGN_NAMED_EMPLOYEE", "ASSIGN_NAMED_ROLE" -> resolveFallback(
                    payload.getJSONObject("fallbackIdentityReference"), emptyPolicy, context, diagnostics
            );
            case "BLOCK" -> throw new IllegalStateException("候选解析结果为空，已失败关闭");
            default -> throw new IllegalArgumentException("空候选策略不合法：" + emptyPolicy);
        };
    }

    private ResolvedCandidateSnapshot resolveFallback(
            JSONObject fallback,
            String emptyPolicy,
            CandidateResolutionContext context,
            List<CandidateDiagnostic> diagnostics
    ) {
        if (fallback == null) {
            throw new IllegalStateException("空候选兜底缺少冻结身份引用");
        }
        ResolvedCandidateSnapshot fallbackSnapshot = switch (emptyPolicy) {
            case "ASSIGN_NAMED_EMPLOYEE" -> resolveEmployees(List.of(
                    requiredId(fallback, "stableId", "兜底员工引用无效")
            ));
            case "ASSIGN_NAMED_ROLE" -> resolveRole(requiredId(fallback, "stableId", "兜底角色引用无效"));
            default -> throw new IllegalArgumentException("空候选兜底类型不合法");
        };
        if (fallbackSnapshot.members().isEmpty()) {
            throw new IllegalStateException("空候选兜底解析仍为空");
        }
        diagnostics.add(new CandidateDiagnostic("EMPTY_FALLBACK_ASSIGNED", "空候选已应用命名兜底", null));
        return applyCandidatePolicies(
                JSON.parseObject("{\"selfApprovalPolicy\":\"BLOCK\",\"emptyCandidatePolicy\":\"BLOCK\"}"),
                context,
                new ResolvedCandidateSnapshot(fallbackSnapshot.members(), diagnostics)
        );
    }

    private List<CandidateDiagnostic> appendDiagnostic(
            List<CandidateDiagnostic> diagnostics,
            String code,
            String message
    ) {
        List<CandidateDiagnostic> result = new ArrayList<>(diagnostics);
        result.add(new CandidateDiagnostic(code, message, null));
        return result;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private ResolvedCandidateSnapshot resolveEmployees(List<Long> employeeIds) {
        return resolveEmployees(employeeIds, false);
    }

    private ResolvedCandidateSnapshot resolveEmployees(List<Long> employeeIds, boolean preserveOrder) {
        Set<Long> sortedEmployeeIds = preserveOrder ? new LinkedHashSet<>() : new TreeSet<>();
        if (employeeIds != null) {
            employeeIds.stream().filter(id -> id != null && id > 0).forEach(sortedEmployeeIds::add);
        }
        List<ResolvedCandidateMember> members = new ArrayList<>();
        List<CandidateDiagnostic> diagnostics = new ArrayList<>();
        for (Long employeeId : sortedEmployeeIds) {
            try {
                BpmEmployeeSnapshot employee = identityGateway.requireEmployee(employeeId);
                if (employee == null) {
                    throw new IllegalArgumentException("员工不存在，employeeId=" + employeeId);
                }
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
