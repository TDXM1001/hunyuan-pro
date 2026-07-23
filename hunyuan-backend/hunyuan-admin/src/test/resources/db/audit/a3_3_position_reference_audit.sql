-- A3.3 岗位引用只读审计脚本。
-- 本脚本仅允许在开发库、隔离副本或经批准的目标库执行只读查询。
-- 员工目标岗位尚未经过业务确认前，禁止据此直接修改 t_employee.position_id。

-- 1. 核对当前数据库和最新成功 Flyway 版本。
SELECT
    DATABASE() AS database_name,
    version,
    description,
    script,
    installed_on
FROM flyway_schema_history
WHERE success = 1
ORDER BY installed_rank DESC
LIMIT 1;

-- 2. 列出当前全部有效岗位，供业务负责人选择目标岗位。
SELECT
    position_id,
    position_name,
    position_level,
    sort,
    remark
FROM t_position
WHERE deleted_flag = 0
ORDER BY sort, position_id;

-- 3. 按岗位编号汇总未删除员工引用，并标识岗位是否仍然有效。
SELECT
    employee.position_id,
    COUNT(*) AS employee_count,
    CASE
        WHEN position.position_id IS NULL THEN '悬空引用'
        ELSE '有效引用'
    END AS reference_status,
    position.position_name
FROM t_employee employee
LEFT JOIN t_position position
    ON position.position_id = employee.position_id
   AND position.deleted_flag = 0
WHERE employee.deleted_flag = 0
  AND employee.position_id IS NOT NULL
GROUP BY
    employee.position_id,
    position.position_id,
    position.position_name
ORDER BY employee.position_id;

-- 4. 输出需要人工确认目标岗位的悬空引用明细。
-- 审计结果不得包含登录密码、手机号或邮箱。
SELECT
    employee.employee_id,
    employee.login_name,
    employee.actual_name,
    employee.department_id,
    employee.position_id AS original_position_id,
    employee.update_time
FROM t_employee employee
WHERE employee.deleted_flag = 0
  AND employee.position_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM t_position position
      WHERE position.position_id = employee.position_id
        AND position.deleted_flag = 0
  )
ORDER BY employee.employee_id;

-- 5. 输出修复前后的对账总数。
SELECT
    COUNT(*) AS non_deleted_employee_count,
    SUM(position_id IS NOT NULL) AS employee_position_reference_count,
    SUM(
        position_id IS NOT NULL
        AND NOT EXISTS (
            SELECT 1
            FROM t_position position
            WHERE position.position_id = t_employee.position_id
              AND position.deleted_flag = 0
        )
    ) AS dangling_reference_count
FROM t_employee
WHERE deleted_flag = 0;

-- 6. P2 关闭查询：只有 dangling_reference_count 等于 0 才允许关闭数据修复。
SELECT
    COUNT(*) AS dangling_reference_count
FROM t_employee employee
WHERE employee.deleted_flag = 0
  AND employee.position_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM t_position position
      WHERE position.position_id = employee.position_id
        AND position.deleted_flag = 0
  );
