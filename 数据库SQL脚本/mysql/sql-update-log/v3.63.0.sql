-- BPM 审批规则可视化配置：补齐候选/审批/范围相关字典。
-- 执行前提：已执行 v3.62.0；v3.47.0 / v3.54.0 中已有 BPM_CANDIDATE_RESOLVER_TYPE、BPM_APPROVAL_COMPLETION_MODE、BPM_POLICY_TYPE、BPM_POLICY_LIFECYCLE_STATE。
-- 兼容影响：仅新增字典与字典项；重复执行不覆盖已人工维护的数据。
-- 恢复方式：停用字典项后可按需删除对应 dict_data；确认无引用后方可删除新增字典。

INSERT INTO `t_dict` (`dict_name`, `dict_code`, `remark`, `disabled_flag`)
SELECT source.`dict_name`, source.`dict_code`, source.`remark`, 0
FROM (
    SELECT 'BPM空候选人策略' AS `dict_name`,
           'BPM_EMPTY_CANDIDATE_POLICY' AS `dict_code`,
           '审批人解析结果为空时的处理策略' AS `remark`
    UNION ALL SELECT 'BPM自审策略', 'BPM_SELF_APPROVAL_POLICY', '发起人也是审批人时的处理策略'
    UNION ALL SELECT 'BPM策略范围类型', 'BPM_POLICY_SCOPE_TYPE', '发起范围与可见范围的圈选类型'
    UNION ALL SELECT 'BPM审批拒绝规则', 'BPM_APPROVAL_REJECTION_RULE', '审批方式中的拒绝结束条件'
    UNION ALL SELECT 'BPM审批允许动作', 'BPM_APPROVAL_ALLOWED_ACTION', '审批方式中允许的人工动作'
) source
WHERE NOT EXISTS (
    SELECT 1
    FROM `t_dict` existing
    WHERE existing.`dict_code` = source.`dict_code`
);

INSERT INTO `t_dict_data`
(`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`)
SELECT dict.`dict_id`, source.`data_value`, source.`data_label`, source.`remark`, source.`sort_order`, 0
FROM (
    SELECT 'BPM_EMPTY_CANDIDATE_POLICY' AS `dict_code`,
           'BLOCK' AS `data_value`,
           '阻断流程' AS `data_label`,
           'BLOCK' AS `remark`,
           100 AS `sort_order`
    UNION ALL SELECT 'BPM_EMPTY_CANDIDATE_POLICY', 'AUTO_REJECT', '自动拒绝', 'AUTO_REJECT', 90
    UNION ALL SELECT 'BPM_EMPTY_CANDIDATE_POLICY', 'AUTO_APPROVE', '自动通过（高风险）', 'AUTO_APPROVE', 80

    UNION ALL SELECT 'BPM_SELF_APPROVAL_POLICY', 'BLOCK', '阻断流程', 'BLOCK', 100
    UNION ALL SELECT 'BPM_SELF_APPROVAL_POLICY', 'SKIP_SELF', '跳过本人', 'SKIP_SELF', 90
    UNION ALL SELECT 'BPM_SELF_APPROVAL_POLICY', 'ASSIGN_DEPARTMENT_MANAGER', '转部门负责人', 'ASSIGN_DEPARTMENT_MANAGER', 80
    UNION ALL SELECT 'BPM_SELF_APPROVAL_POLICY', 'ALLOW', '允许自审（高风险）', 'ALLOW', 70

    UNION ALL SELECT 'BPM_POLICY_SCOPE_TYPE', 'ALL', '全部员工', 'ALL', 100
    UNION ALL SELECT 'BPM_POLICY_SCOPE_TYPE', 'EMPLOYEE_IDS', '指定员工', 'EMPLOYEE_IDS', 90
    UNION ALL SELECT 'BPM_POLICY_SCOPE_TYPE', 'ROLE_IDS', '指定角色', 'ROLE_IDS', 80
    UNION ALL SELECT 'BPM_POLICY_SCOPE_TYPE', 'DEPARTMENT_IDS', '指定部门', 'DEPARTMENT_IDS', 70

    UNION ALL SELECT 'BPM_APPROVAL_REJECTION_RULE', 'IMMEDIATE', '任一拒绝立即结束', 'IMMEDIATE', 100
    UNION ALL SELECT 'BPM_APPROVAL_REJECTION_RULE', 'WHEN_APPROVAL_UNREACHABLE', '无法达到通过条件时结束', 'WHEN_APPROVAL_UNREACHABLE', 90

    UNION ALL SELECT 'BPM_APPROVAL_ALLOWED_ACTION', 'APPROVE', '通过', 'APPROVE', 100
    UNION ALL SELECT 'BPM_APPROVAL_ALLOWED_ACTION', 'REJECT', '拒绝', 'REJECT', 90
    UNION ALL SELECT 'BPM_APPROVAL_ALLOWED_ACTION', 'RETURN', '退回', 'RETURN', 80
) source
JOIN `t_dict` dict
  ON dict.`dict_code` = source.`dict_code`
LEFT JOIN `t_dict_data` existing
  ON existing.`dict_id` = dict.`dict_id`
 AND existing.`data_value` = source.`data_value`
WHERE existing.`dict_data_id` IS NULL;
