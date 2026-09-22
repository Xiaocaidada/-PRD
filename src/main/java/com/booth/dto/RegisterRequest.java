package com.booth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 商户注册请求
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "商户负责人姓名不能为空")
    private String name;

    @NotNull(message = "商品品类至少选择一项")
    @Size(min = 1, message = "商品品类至少选择一项")
    private List<String> categories;

    @NotBlank(message = "营业执照号码不能为空")
    private String licenseNo;

    @NotBlank(message = "手机号码不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号码格式不正确")
    private String phone;

    @NotBlank(message = "短信验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
    private String smsCode;

    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码至少 8 位")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
