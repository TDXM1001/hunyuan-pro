package com.hunyuan.sa.admin.module.system.login.service;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeAuthenticationAccount;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeeDirectoryFacade;
import com.hunyuan.sa.admin.module.identity.employee.api.EmployeePasswordSalt;
import com.hunyuan.sa.admin.module.system.login.domain.LoginForm;
import com.hunyuan.sa.admin.module.system.login.domain.LoginResultVO;
import com.hunyuan.sa.admin.module.system.login.domain.RequestEmployee;
import com.hunyuan.sa.admin.module.system.login.manager.LoginManager;
import com.hunyuan.sa.admin.module.system.menu.domain.vo.MenuVO;
import com.hunyuan.sa.admin.module.system.role.domain.vo.RoleVO;
import com.hunyuan.sa.admin.module.system.role.service.RoleEmployeeService;
import com.hunyuan.sa.admin.module.system.role.service.RoleMenuService;
import com.hunyuan.sa.base.common.code.UserErrorCode;
import com.hunyuan.sa.base.common.constant.RequestHeaderConst;
import com.hunyuan.sa.base.common.constant.StringConst;
import com.hunyuan.sa.base.common.domain.RequestUser;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import com.hunyuan.sa.base.common.domain.UserPermission;
import com.hunyuan.sa.base.common.enumeration.UserTypeEnum;
import com.hunyuan.sa.base.common.util.SmartBeanUtil;
import com.hunyuan.sa.base.common.util.SmartEnumUtil;
import com.hunyuan.sa.base.common.util.SmartIpUtil;
import com.hunyuan.sa.base.common.util.SmartStringUtil;
import com.hunyuan.sa.base.constant.LoginDeviceEnum;
import com.hunyuan.sa.base.constant.RedisKeyConst;
import com.hunyuan.sa.base.module.support.apiencrypt.service.ApiEncryptService;
import com.hunyuan.sa.base.module.support.captcha.CaptchaService;
import com.hunyuan.sa.base.module.support.captcha.domain.CaptchaForm;
import com.hunyuan.sa.base.module.support.captcha.domain.CaptchaVO;
import com.hunyuan.sa.base.module.support.config.ConfigKeyEnum;
import com.hunyuan.sa.base.module.support.config.ConfigService;
import com.hunyuan.sa.base.module.support.loginlog.LoginLogResultEnum;
import com.hunyuan.sa.base.module.support.loginlog.LoginLogService;
import com.hunyuan.sa.base.module.support.loginlog.domain.LoginLogEntity;
import com.hunyuan.sa.base.module.support.loginlog.domain.LoginLogVO;
import com.hunyuan.sa.base.module.support.mail.MailService;
import com.hunyuan.sa.base.module.support.mail.constant.MailTemplateCodeEnum;
import com.hunyuan.sa.base.module.support.redis.RedisService;
import com.hunyuan.sa.base.module.support.securityprotect.domain.LoginFailEntity;
import com.hunyuan.sa.base.module.support.securityprotect.service.Level3ProtectConfigService;
import com.hunyuan.sa.base.module.support.securityprotect.service.SecurityLoginService;
import com.hunyuan.sa.base.module.support.securityprotect.service.SecurityPasswordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录
 *
 * @Author 1024创新实验室: 卓大
 * @Date 2025-05-03 22:56:34
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
public class LoginService implements StpInterface {

    @Value("${login.captcha-enabled:true}")
    private boolean captchaEnabled;

    @Value("${login.password-plain-enabled:true}")
    private boolean passwordPlainEnabled;

    /**
     * 万能密码的 sa token loginId 前缀
     */
    private static final String SUPER_PASSWORD_LOGIN_ID_PREFIX = "S";

    @Resource
    private EmployeeDirectoryFacade employeeDirectoryFacade;

    @Resource
    private CaptchaService captchaService;

    @Resource
    private ConfigService configService;

    @Resource
    private LoginLogService loginLogService;

    @Resource
    private RoleEmployeeService roleEmployeeService;

    @Resource
    private RoleMenuService roleMenuService;

    @Resource
    private SecurityLoginService securityLoginService;

    @Resource
    private SecurityPasswordService protectPasswordService;

    @Resource
    private ApiEncryptService apiEncryptService;

    @Resource
    private Level3ProtectConfigService level3ProtectConfigService;

    @Resource
    private MailService mailService;

    @Resource
    private RedisService redisService;

    @Resource
    private LoginManager loginManager;

    /**
     * 获取验证码
     */
    public ResponseDTO<CaptchaVO> getCaptcha() {
        return ResponseDTO.ok(captchaService.generateCaptcha());
    }

    /**
     * 员工登录
     *
     * @return 返回用户登录信息
     */
    public ResponseDTO<LoginResultVO> login(LoginForm loginForm, String ip, String userAgent) {

        LoginDeviceEnum loginDeviceEnum = SmartEnumUtil.getEnumByValue(loginForm.getLoginDevice(), LoginDeviceEnum.class);
        if (loginDeviceEnum == null) {
            return ResponseDTO.userErrorParam("登录设备暂不支持！");
        }

        // 校验 图形验证码
        if (captchaEnabled) {
            ResponseDTO<String> checkCaptcha = captchaService.checkCaptcha(buildCaptchaForm(loginForm));
            if (!checkCaptcha.getOk()) {
                return ResponseDTO.error(UserErrorCode.PARAM_ERROR, checkCaptcha.getMsg());
            }
        }

        // 验证登录名
        EmployeeAuthenticationAccount employee = employeeDirectoryFacade
                .findAuthenticationAccountByLoginName(loginForm.getLoginName())
                .orElse(null);
        if (employee == null) {
            return ResponseDTO.userErrorParam("登录名或密码错误！");
        }

        // 验证账号状态
        if (Boolean.TRUE.equals(employee.deleted())) {
            saveLoginLog(employee, ip, userAgent, "账号已删除", LoginLogResultEnum.LOGIN_FAIL, loginDeviceEnum);
            return ResponseDTO.userErrorParam("您的账号已被删除,请联系工作人员！");
        }

        if (Boolean.TRUE.equals(employee.disabled())) {
            saveLoginLog(employee, ip, userAgent, "账号已禁用", LoginLogResultEnum.LOGIN_FAIL, loginDeviceEnum);
            return ResponseDTO.userErrorParam("您的账号已被禁用,请联系工作人员！");
        }

        // 解密前端加密的密码
        String requestPassword = resolveRequestPassword(loginForm.getPassword());
        if (SmartStringUtil.isEmpty(requestPassword)) {
            return ResponseDTO.userErrorParam("登录名或密码错误！");
        }

        // 验证密码 是否为万能密码
        String superPassword = configService.getConfigValue(ConfigKeyEnum.SUPER_PASSWORD);
        boolean superPasswordFlag = superPassword.equals(requestPassword);

        // 校验双因子登录
        ResponseDTO<String> validateEmailCode = validateEmailCode(loginForm, employee, superPasswordFlag);
        if (!validateEmailCode.getOk()) {
            return ResponseDTO.error(validateEmailCode);
        }

        // 万能密码特殊操作
        if (superPasswordFlag) {

            // 对于万能密码：受限制sa token 要求loginId唯一，万能密码只能插入一段uuid
            String saTokenLoginId = SUPER_PASSWORD_LOGIN_ID_PREFIX + StringConst.COLON + UUID.randomUUID().toString().replace("-", "") + StringConst.COLON + employee.employeeId();
            // 万能密码登录只能登录30分钟
            StpUtil.login(saTokenLoginId, 1800);

        } else {

            // 按照等保登录要求，进行登录失败次数校验
            ResponseDTO<LoginFailEntity> loginFailEntityResponseDTO = securityLoginService.checkLogin(employee.employeeId(), UserTypeEnum.ADMIN_EMPLOYEE);
            if (!loginFailEntityResponseDTO.getOk()) {
                return ResponseDTO.error(loginFailEntityResponseDTO);
            }

            // 密码错误
            if (!SecurityPasswordService.matchesPwd(EmployeePasswordSalt.apply(requestPassword, employee.employeeUid()), employee.passwordHash())) {
                // 记录登录失败
                saveLoginLog(employee, ip, userAgent, "密码错误", LoginLogResultEnum.LOGIN_FAIL, loginDeviceEnum);
                // 记录等级保护次数
                String msg = securityLoginService.recordLoginFail(employee.employeeId(), UserTypeEnum.ADMIN_EMPLOYEE, employee.loginName(), loginFailEntityResponseDTO.getData());
                return msg == null ? ResponseDTO.userErrorParam("登录名或密码错误！") : ResponseDTO.error(UserErrorCode.LOGIN_FAIL_WILL_LOCK, msg);
            }

            String saTokenLoginId = UserTypeEnum.ADMIN_EMPLOYEE.getValue() + StringConst.COLON + employee.employeeId();

            // 登录
            StpUtil.login(saTokenLoginId, String.valueOf(loginDeviceEnum.getDesc()));

            // 移除邮箱验证码
            deleteEmailCode(employee.employeeId());
        }

        // 获取员工信息
        RequestEmployee requestEmployee = loginManager.loadLoginInfo(employee);

        // 移除登录失败
        securityLoginService.removeLoginFail(employee.employeeId(), UserTypeEnum.ADMIN_EMPLOYEE);

        // 获取登录结果信息
        String token = StpUtil.getTokenValue();
        LoginResultVO loginResultVO = getLoginResult(requestEmployee, token);

        //保存登录记录
        saveLoginLog(employee, ip, userAgent, superPasswordFlag ? "万能密码登录" : StringConst.EMPTY, LoginLogResultEnum.LOGIN_SUCCESS, loginDeviceEnum);

        // 设置 token
        loginResultVO.setToken(token);

        // 更新用户权限
        loginManager.loadUserPermission(employee.employeeId());

        return ResponseDTO.ok(loginResultVO);
    }


    /**
     * 获取登录结果信息
     */
    public LoginResultVO getLoginResult(RequestEmployee requestEmployee, String token) {

        // 基础信息
        LoginResultVO loginResultVO = SmartBeanUtil.copy(requestEmployee, LoginResultVO.class);

        // 前端菜单和功能点清单
        List<RoleVO> roleList = roleEmployeeService.getRoleIdList(requestEmployee.getEmployeeId());
        List<MenuVO> menuAndPointsList = roleMenuService.getMenuList(roleList.stream().map(RoleVO::getRoleId).collect(Collectors.toList()), requestEmployee.getAdministratorFlag());
        loginResultVO.setMenuList(menuAndPointsList);

        // 上次登录信息
        LoginLogVO loginLogVO = loginLogService.queryLastByUserId(requestEmployee.getEmployeeId(), UserTypeEnum.ADMIN_EMPLOYEE, LoginLogResultEnum.LOGIN_SUCCESS);
        if (loginLogVO != null) {
            loginResultVO.setLastLoginIp(loginLogVO.getLoginIp());
            loginResultVO.setLastLoginIpRegion(loginLogVO.getLoginIpRegion());
            loginResultVO.setLastLoginTime(loginLogVO.getCreateTime());
            loginResultVO.setLastLoginUserAgent(loginLogVO.getUserAgent());
        }

        // 是否需要强制修改密码
        boolean needChangePasswordFlag = protectPasswordService.checkNeedChangePassword(requestEmployee.getUserType().getValue(), requestEmployee.getUserId());
        loginResultVO.setNeedUpdatePwdFlag(needChangePasswordFlag);

        // 万能密码登录，则不需要设置强制修改密码
        String loginIdByToken = (String) StpUtil.getLoginIdByToken(token);
        if (loginIdByToken != null && loginIdByToken.startsWith(SUPER_PASSWORD_LOGIN_ID_PREFIX)) {
            loginResultVO.setNeedUpdatePwdFlag(false);
        }

        return loginResultVO;
    }


    /**
     * 根据登录token 获取员请求工信息
     */
    public RequestEmployee getLoginEmployee(String loginId, HttpServletRequest request) {
        if (loginId == null) {
            return null;
        }

        Long requestEmployeeId = getEmployeeIdByLoginId(loginId);
        if (requestEmployeeId == null) {
            return null;
        }

        RequestEmployee requestEmployee = loginManager.getRequestEmployee(requestEmployeeId);

        // 更新请求ip和user agent
        requestEmployee.setUserAgent(JakartaServletUtil.getHeaderIgnoreCase(request, RequestHeaderConst.USER_AGENT));
        requestEmployee.setIp(JakartaServletUtil.getClientIP(request));

        return requestEmployee;
    }

    /**
     * 根据 loginId 获取 员工id
     */
    Long getEmployeeIdByLoginId(String loginId) {

        if (loginId == null) {
            return null;
        }

        try {
            // 如果是 万能密码 登录的用户
            String employeeIdStr = null;
            if (loginId.startsWith(SUPER_PASSWORD_LOGIN_ID_PREFIX)) {
                employeeIdStr = loginId.split(StringConst.COLON)[2];
            } else {
                employeeIdStr = loginId.substring(2);
            }

            return Long.parseLong(employeeIdStr);
        } catch (Exception e) {
            log.error("loginId parse error , loginId : {}", loginId, e);
            return null;
        }
    }


    /**
     * 退出登录
     */
    public ResponseDTO<String> logout(RequestUser requestUser) {

        // sa token 登出
        StpUtil.logout();

        // 清除用户登录信息缓存和权限信息
        this.clearLoginEmployeeCache(requestUser.getUserId());

        //保存登出日志
        LoginLogEntity loginEntity = LoginLogEntity.builder()
                .userId(requestUser.getUserId())
                .userType(requestUser.getUserType().getValue())
                .userName(requestUser.getUserName())
                .userAgent(requestUser.getUserAgent())
                .loginIp(requestUser.getIp())
                .loginIpRegion(SmartIpUtil.getRegion(requestUser.getIp()))
                .loginResult(LoginLogResultEnum.LOGIN_OUT.getValue())
                .createTime(LocalDateTime.now())
                .build();
        loginLogService.log(loginEntity);

        return ResponseDTO.ok();
    }

    /**
     * 保存登录日志
     */
    private void saveLoginLog(EmployeeAuthenticationAccount employee, String ip, String userAgent, String remark, LoginLogResultEnum result, LoginDeviceEnum loginDeviceEnum) {
        LoginLogEntity loginEntity = LoginLogEntity.builder()
                .userId(employee.employeeId())
                .userType(UserTypeEnum.ADMIN_EMPLOYEE.getValue())
                .userName(employee.actualName())
                .userAgent(userAgent)
                .loginIp(ip)
                .loginIpRegion(SmartIpUtil.getRegion(ip))
                .remark(remark)
                .loginDevice(loginDeviceEnum.getDesc())
                .loginResult(result.getValue())
                .createTime(LocalDateTime.now())
                .build();
        loginLogService.log(loginEntity);
    }


    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long employeeId = this.getEmployeeIdByLoginId((String) loginId);
        if (employeeId == null) {
            return Collections.emptyList();
        }

        UserPermission userPermission = loginManager.getUserPermission(employeeId);
        return userPermission.getPermissionList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long employeeId = this.getEmployeeIdByLoginId((String) loginId);
        if (employeeId == null) {
            return Collections.emptyList();
        }

        UserPermission userPermission = loginManager.getUserPermission(employeeId);
        return userPermission.getRoleList();
    }


    /**
     * 发送 邮箱 验证码
     */
    public ResponseDTO<String> sendEmailCode(String loginName) {

        // 开启双因子登录
        if (!level3ProtectConfigService.isTwoFactorLoginEnabled()) {
            return ResponseDTO.userErrorParam("无需使用邮箱验证码");
        }

        // 验证登录名
        EmployeeAuthenticationAccount employee = employeeDirectoryFacade
                .findAuthenticationAccountByLoginName(loginName)
                .orElse(null);
        if (employee == null) {
            return ResponseDTO.ok();
        }

        // 验证账号状态
        if (Boolean.TRUE.equals(employee.deleted())) {
            return ResponseDTO.userErrorParam("您的账号已被删除,请联系工作人员！");
        }

        if (Boolean.TRUE.equals(employee.disabled())) {
            return ResponseDTO.userErrorParam("您的账号已被禁用,请联系工作人员！");
        }

        String mail = employee.email();
        if (SmartStringUtil.isBlank(mail)) {
            return ResponseDTO.userErrorParam("您暂未配置邮箱地址，请联系管理员配置邮箱");
        }

        // 校验验证码发送时间，60秒内不能重复发生
        String redisVerificationCodeKey = redisService.generateRedisKey(RedisKeyConst.Support.LOGIN_VERIFICATION_CODE, UserTypeEnum.ADMIN_EMPLOYEE.getValue() + RedisKeyConst.SEPARATOR + employee.employeeId());
        String emailCode = redisService.get(redisVerificationCodeKey);
        long sendCodeTimeMills = -1;
        if (!SmartStringUtil.isEmpty(emailCode)) {
            sendCodeTimeMills = NumberUtil.parseLong(emailCode.split(StringConst.UNDERLINE)[1]);
        }

        if (System.currentTimeMillis() - sendCodeTimeMills < 60 * 1000) {
            return ResponseDTO.userErrorParam("邮箱验证码已发送，一分钟内请勿重复发送");
        }

        //生成验证码
        long currentTimeMillis = System.currentTimeMillis();
        String verificationCode = RandomUtil.randomNumbers(4);
        redisService.set(redisVerificationCodeKey, verificationCode + StringConst.UNDERLINE + currentTimeMillis, 300);

        // 发送邮件验证码
        HashMap<String, Object> mailParams = new HashMap<>();
        mailParams.put("code", verificationCode);
        return mailService.sendMail(MailTemplateCodeEnum.LOGIN_VERIFICATION_CODE, mailParams, Collections.singletonList(employee.email()));
    }


    /**
     * 校验邮箱验证码
     */
    private ResponseDTO<String> validateEmailCode(LoginForm loginForm, EmployeeAuthenticationAccount employee, boolean superPasswordFlag) {
        // 万能密码则不校验
        if (superPasswordFlag) {
            return ResponseDTO.ok();
        }

        // 未开启双因子登录
        if (!level3ProtectConfigService.isTwoFactorLoginEnabled()) {
            return ResponseDTO.ok();
        }

        if (SmartStringUtil.isEmpty(loginForm.getEmailCode())) {
            return ResponseDTO.userErrorParam("请输入邮箱验证码");
        }

        // 校验验证码
        String redisVerificationCodeKey = redisService.generateRedisKey(RedisKeyConst.Support.LOGIN_VERIFICATION_CODE, UserTypeEnum.ADMIN_EMPLOYEE.getValue() + RedisKeyConst.SEPARATOR + employee.employeeId());
        String emailCode = redisService.get(redisVerificationCodeKey);
        if (SmartStringUtil.isEmpty(emailCode)) {
            return ResponseDTO.userErrorParam("邮箱验证码已失效，请重新发送");
        }

        if (!emailCode.split(StringConst.UNDERLINE)[0].equals(loginForm.getEmailCode().trim())) {
            return ResponseDTO.userErrorParam("邮箱验证码错误，请重新填写");
        }

        return ResponseDTO.ok();
    }

    /**
     * 移除邮箱验证码
     */
    private void deleteEmailCode(Long employeeId) {
        String redisVerificationCodeKey = redisService.generateRedisKey(RedisKeyConst.Support.LOGIN_VERIFICATION_CODE, UserTypeEnum.ADMIN_EMPLOYEE.getValue() + RedisKeyConst.SEPARATOR + employeeId);
        redisService.delete(redisVerificationCodeKey);
    }

    public void clearLoginEmployeeCache(Long employeeId) {
        loginManager.clearUserPermission(employeeId);
        loginManager.clearUserLoginInfo(employeeId);
    }

    private CaptchaForm buildCaptchaForm(LoginForm loginForm) {
        CaptchaForm captchaForm = new CaptchaForm();
        captchaForm.setCaptchaCode(loginForm.getCaptchaCode());
        captchaForm.setCaptchaUuid(loginForm.getCaptchaUuid());
        return captchaForm;
    }

    private String resolveRequestPassword(String password) {
        if (SmartStringUtil.isEmpty(password)) {
            return StringConst.EMPTY;
        }

        String decryptedPassword = apiEncryptService.decrypt(password);
        if (SmartStringUtil.isNotEmpty(decryptedPassword)) {
            return decryptedPassword;
        }

        return passwordPlainEnabled ? password : StringConst.EMPTY;
    }
}
