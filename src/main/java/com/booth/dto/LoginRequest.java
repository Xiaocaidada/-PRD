package com.booth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 商户登录请求
 */
@Data
public class LoginRequest {

    /** 证件号或手机号 */
    @NotBlank(message = "证件号/手机号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "图形验证码不能为空")
    private String captchaId;

    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;
}
