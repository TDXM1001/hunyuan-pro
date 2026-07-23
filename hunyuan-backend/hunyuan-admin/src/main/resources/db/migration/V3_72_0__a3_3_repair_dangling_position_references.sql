-- A3.3 P2：清理两条已停用账号遗留的悬空岗位引用。
--
-- 业务依据：
-- 1. 员工 66（罗伊）与 67（初晓）均已停用；
-- 2. 两条记录都引用已经不存在的岗位 2；
-- 3. 当前岗位目录和历史可追溯备份均无法证明岗位 2 应映射到哪个有效岗位。
--
-- 因此采用最小修复：只清空已停用账号的无效岗位引用，不猜测新的岗位归属。
-- 登录名、姓名、停用状态和原岗位编号共同作为保护条件，避免主键被其他数据复用时误改。

UPDATE `t_employee`
SET `position_id` = NULL
WHERE `employee_id` = 66
  AND `login_name` = 'luoyi'
  AND `actual_name` = '罗伊'
  AND `disabled_flag` = 1
  AND `deleted_flag` = 0
  AND `position_id` = 2
  AND NOT EXISTS (
    SELECT 1
    FROM `t_position`
    WHERE `position_id` = 2
      AND `deleted_flag` = 0
  );

UPDATE `t_employee`
SET `position_id` = NULL
WHERE `employee_id` = 67
  AND `login_name` = 'chuxiao'
  AND `actual_name` = '初晓'
  AND `disabled_flag` = 1
  AND `deleted_flag` = 0
  AND `position_id` = 2
  AND NOT EXISTS (
    SELECT 1
    FROM `t_position`
    WHERE `position_id` = 2
      AND `deleted_flag` = 0
  );
