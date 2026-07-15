-- BPM 业务对象配置：补充业务类型字典，供业务对象、流程绑定和运行时申请页面统一选择。

INSERT INTO `t_dict` (`dict_name`, `dict_code`, `remark`, `disabled_flag`)
SELECT source.`dict_name`, source.`dict_code`, source.`remark`, 0
FROM (
    SELECT 'BPM业务类型' AS `dict_name`,
           'BPM_BUSINESS_TYPE' AS `dict_code`,
           '业务对象、流程绑定与运行时申请统一使用的业务类型编码' AS `remark`
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
    SELECT 'BPM_BUSINESS_TYPE' AS `dict_code`,
           'GENERIC_APPLICATION' AS `data_value`,
           '通用申请' AS `data_label`,
           '默认业务对象类型，适用于当前 BPM 通用申请闭环' AS `remark`,
           100 AS `sort_order`
) source
JOIN `t_dict` dict
  ON dict.`dict_code` = source.`dict_code`
LEFT JOIN `t_dict_data` existing
  ON existing.`dict_id` = dict.`dict_id`
 AND existing.`data_value` = source.`data_value`
WHERE existing.`dict_data_id` IS NULL;
