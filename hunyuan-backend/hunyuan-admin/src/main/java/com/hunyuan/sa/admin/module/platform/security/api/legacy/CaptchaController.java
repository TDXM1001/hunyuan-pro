package com.hunyuan.sa.admin.module.platform.security.api.legacy;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import com.hunyuan.sa.base.common.controller.SupportBaseController;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.constant.SwaggerTagConst;
import com.hunyuan.sa.base.module.support.captcha.api.PlatformCaptchaChallenge;
import com.hunyuan.sa.base.module.support.captcha.api.PlatformCaptchaFacade;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图形验证码业务
 *
 * @Author 1024创新实验室: 胡克
 * @Date 2021-09-02 20:21:10
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = SwaggerTagConst.Support.CAPTCHA)
@RestController
public class CaptchaController extends SupportBaseController {

    @Resource
    private PlatformCaptchaFacade platformCaptchaFacade;

    @Operation(summary = "获取图形验证码 @author 胡克")
    @GetMapping("/captcha")
    public ResponseDTO<PlatformCaptchaChallenge> generateCaptcha() {
        return platformCaptchaFacade.generateChallenge();
    }

}
