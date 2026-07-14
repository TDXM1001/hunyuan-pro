package com.hunyuan.sa.bpm.module.candidate.domain.vo;

import java.util.List;

public record BpmIdentityOptionPageVO(
        List<BpmIdentityOptionVO> items, long total, int pageNum, int pageSize
) {
}
