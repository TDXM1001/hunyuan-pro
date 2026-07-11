package com.hunyuan.sa.bpm.engine.compiler;

/**
 * 流程模型解析失败，code 可稳定进入发布报告。
 */
public class ProcessModelParseException extends IllegalArgumentException {

    private final String code;

    public ProcessModelParseException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
