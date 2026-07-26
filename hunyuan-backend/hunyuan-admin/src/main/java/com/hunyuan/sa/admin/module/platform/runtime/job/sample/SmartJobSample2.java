package com.hunyuan.sa.admin.module.platform.runtime.job.sample;

import lombok.extern.slf4j.Slf4j;
import com.hunyuan.sa.base.module.support.job.core.SmartJob;
import org.springframework.stereotype.Service;

/**
 * 定时任务 示例2
 *
 * @author huke
 * @date 2024/6/17 21:30
 */
@Slf4j
@Service
public class SmartJobSample2 implements SmartJob {

    /**
     * 定时任务示例
     * 需要事务时 添加 @Transactional 注解
     *
     * @param param 可选参数 任务不需要时不用管
     * @return
     */
    @Override
    public String run(String param) {
        // 示例任务不得修改其他 owner 的配置表，真实任务应通过所属模块公开用例处理自己的数据。
        log.info("SmartJob Sample2 executed, param: {}", param);
        return "执行成功,本次处理数据0条";
    }

}
