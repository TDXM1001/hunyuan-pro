package com.hunyuan.sa.base.module.support.mail.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 平台模板邮件发送命令。
 *
 * @param templateCode 模板编码
 * @param templateParameters 模板参数
 * @param recipients 收件人邮箱列表
 */
public record PlatformTemplateMailCommand(
        PlatformMailTemplateCode templateCode,
        Map<String, Object> templateParameters,
        List<String> recipients) {

    /**
     * 固化命令快照，避免调用方在发送过程中修改参数或收件人。
     */
    public PlatformTemplateMailCommand {
        Objects.requireNonNull(templateCode, "邮件模板编码不能为空");
        Objects.requireNonNull(templateParameters, "邮件模板参数不能为空");
        Objects.requireNonNull(recipients, "邮件收件人不能为空");
        templateParameters = Map.copyOf(templateParameters);
        recipients = List.copyOf(recipients);
    }
}
