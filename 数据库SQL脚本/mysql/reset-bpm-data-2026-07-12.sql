-- Hunyuan BPM 开发/测试数据重置脚本
-- 日期：2026-07-12
--
-- 使用范围：仅用于开发或测试库。执行前请在数据库客户端中显式选中目标库。
--
-- 清理范围：
--   1. 当前库全部 t_bpm_* 表中的数据；
--   2. 当前库全部 Flowable ACT_* 表中的流程定义、运行、历史、事件、表单和 identity 数据。
--
-- 保留范围：
--   1. 所有表结构、索引和 SQL 迁移记录；
--   2. act_ge_property（Flowable 引擎 schema 元数据）；
--   3. Hunyuan 系统用户、部门、角色、菜单及其他非 BPM 表。
--
-- 说明：
--   - 本脚本会立即清理当前选中库的全部 BPM 数据；执行前必须确认目标库无误。
--   - 脚本使用 DELETE + 事务；保留自增 ID 的当前计数，不重置表结构。
--   - 若目标库中存在其他 Flowable 产品数据，本脚本也会清理；请勿在共享生产库执行。

SELECT DATABASE() AS current_database;

-- 已确认执行开关：本文件完整执行时会清理当前选中库的 BPM 数据。
SET @hunyuan_bpm_reset_confirm = 'RESET_HUNYUAN_BPM_DATA';

-- 预检：本次会清理的表。InnoDB 的 table_rows 为估算值，仅用于识别范围。
SELECT
    table_name,
    table_rows AS estimated_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_type = 'BASE TABLE'
  AND (
      LEFT(LOWER(table_name), 6) = 't_bpm_'
      OR (
              LEFT(LOWER(table_name), 4) = 'act_'
              AND LOWER(table_name) <> 'act_ge_property'
      )
  )
ORDER BY table_name;

DELIMITER $$

DROP PROCEDURE IF EXISTS hunyuan_reset_bpm_data$$

CREATE PROCEDURE hunyuan_reset_bpm_data()
BEGIN
    DECLARE done TINYINT DEFAULT 0;
    DECLARE target_table VARCHAR(128);
    DECLARE dynamic_sql TEXT;
    DECLARE table_count INT DEFAULT 0;
    DECLARE remaining_rows BIGINT DEFAULT 0;

    DECLARE reset_cursor CURSOR FOR
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_type = 'BASE TABLE'
          AND (
              LEFT(LOWER(table_name), 6) = 't_bpm_'
              OR (
                  LEFT(LOWER(table_name), 4) = 'act_'
                  AND LOWER(table_name) <> 'act_ge_property'
              )
          )
        ORDER BY table_name;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET FOREIGN_KEY_CHECKS = @hunyuan_bpm_reset_previous_fk_checks;
        RESIGNAL;
    END;

    SET @hunyuan_bpm_reset_previous_fk_checks = @@FOREIGN_KEY_CHECKS;

    IF DATABASE() IS NULL OR DATABASE() = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '未选中目标数据库，BPM 数据未清理';
    END IF;

    IF COALESCE(@hunyuan_bpm_reset_confirm, '') <> 'RESET_HUNYUAN_BPM_DATA' THEN
        SELECT
            'RESET_NOT_EXECUTED' AS reset_status,
            '请确认目标库后设置 @hunyuan_bpm_reset_confirm 再执行' AS message;
    ELSE
        SET FOREIGN_KEY_CHECKS = 0;
        START TRANSACTION;

        OPEN reset_cursor;
        delete_loop: LOOP
            FETCH reset_cursor INTO target_table;
            IF done = 1 THEN
                LEAVE delete_loop;
            END IF;

            SET dynamic_sql = CONCAT(
                'DELETE FROM `',
                REPLACE(DATABASE(), '`', '``'),
                '`.`',
                REPLACE(target_table, '`', '``'),
                '`'
            );
            SET @hunyuan_bpm_reset_sql = dynamic_sql;
            PREPARE reset_statement FROM @hunyuan_bpm_reset_sql;
            EXECUTE reset_statement;
            DEALLOCATE PREPARE reset_statement;
            SET table_count = table_count + 1;
        END LOOP;
        CLOSE reset_cursor;

        SET done = 0;
        OPEN reset_cursor;
        verify_loop: LOOP
            FETCH reset_cursor INTO target_table;
            IF done = 1 THEN
                LEAVE verify_loop;
            END IF;

            SET @hunyuan_bpm_reset_remaining = 0;
            SET dynamic_sql = CONCAT(
                'SELECT COUNT(*) INTO @hunyuan_bpm_reset_remaining FROM `',
                REPLACE(DATABASE(), '`', '``'),
                '`.`',
                REPLACE(target_table, '`', '``'),
                '`'
            );
            SET @hunyuan_bpm_reset_sql = dynamic_sql;
            PREPARE verify_statement FROM @hunyuan_bpm_reset_sql;
            EXECUTE verify_statement;
            DEALLOCATE PREPARE verify_statement;

            SET remaining_rows = remaining_rows + @hunyuan_bpm_reset_remaining;
        END LOOP;
        CLOSE reset_cursor;

        IF remaining_rows <> 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'BPM 数据清理校验失败，事务已回滚';
        END IF;

        COMMIT;
        SET FOREIGN_KEY_CHECKS = @hunyuan_bpm_reset_previous_fk_checks;

        SELECT
            'RESET_COMPLETED' AS reset_status,
            DATABASE() AS target_database,
            table_count AS cleared_table_count,
            remaining_rows AS remaining_rows;
    END IF;
END$$

DELIMITER ;

CALL hunyuan_reset_bpm_data();
DROP PROCEDURE hunyuan_reset_bpm_data;
