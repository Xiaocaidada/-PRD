package com.booth.controller;

import com.booth.common.Result;
import com.booth.dto.AdminLoginRequest;
import com.booth.dto.LoginRequest;
import com.booth.dto.RegisterRequest;
import com.booth.service.AuthService;
import com.booth.util.CaptchaUtil;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 认证接口：验证码 / 注册 / 登录
 */
@RestController
@Validated
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 图形验证码 */
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        String id = UUID.randomUUID().toString().replace("-", "");
        String image = CaptchaUtil.generate(id);
        Map<String, String> data = new HashMap<>();
        data.put("captchaId", id);
        data.put("imageBase64", image);
        return Result.ok(data);
    }

    /** 发送短信验证码（演示：返回 devCode 便于测试） */
    @PostMapping("/auth/sms")
    public Result<Map<String, Object>> sendSms(@RequestBody Map<String, String> body) {
        return Result.ok(authService.sendSms(body.getOrDefault("phone", "")));
    }

    /** 商户注册 */
    @PostMapping("/auth/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        return Result.ok("注册成功", authService.register(req));
    }

    /** 商户登录 */
    @PostMapping("/auth/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok("登录成功", authService.login(req));
    }

    /** 管理员登录 */
    @PostMapping("/auth/admin/login")
    public Result<Map<String, Object>> adminLogin(@Valid @RequestBody AdminLoginRequest req) {
        return Result.ok("登录成功", authService.adminLogin(req.getUsername(), req.getPassword()));
    }





}
