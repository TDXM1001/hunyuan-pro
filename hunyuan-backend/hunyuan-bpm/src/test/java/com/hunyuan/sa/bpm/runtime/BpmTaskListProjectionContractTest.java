package com.hunyuan.sa.bpm.runtime;

import com.hunyuan.sa.bpm.module.runtime.domain.vo.BpmTaskVO;
import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class BpmTaskListProjectionContractTest {

    @Test
    void taskListProjectionShouldExposeTaskKey() throws Exception {
        boolean hasTaskKeyProperty = Arrays.stream(
                        Introspector.getBeanInfo(BpmTaskVO.class).getPropertyDescriptors()
                )
                .anyMatch(property -> "taskKey".equals(property.getName()));

        String mapperXml = Files.readString(
                Path.of("src/main/resources/mapper/bpm/runtime/BpmTaskMapper.xml"),
                StandardCharsets.UTF_8
        );

        assertThat(hasTaskKeyProperty).isTrue();
        assertThat(mapperXml).containsOnlyOnce("<select id=\"queryPage\"");
        assertThat(mapperXml).containsOnlyOnce("<select id=\"queryCurrentTasksByInstanceId\"");
        assertThat(mapperXml).containsSubsequence(
                "task_id as taskId,",
                "task_key as taskKey,",
                "instance_id as instanceId"
        );
        assertThat(countOccurrences(mapperXml, "task_key as taskKey")).isEqualTo(2);
    }

    private int countOccurrences(String source, String target) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
    }
}
