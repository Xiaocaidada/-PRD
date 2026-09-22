package com.booth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.booth.common.BusinessException;
import com.booth.dto.LoginRequest;
import com.booth.dto.RegisterRequest;
import com.booth.entity.Admin;
import com.booth.entity.Merchant;
import com.booth.mapper.AdminMapper;
import com.booth.mapper.MerchantMapper;
import com.booth.util.CaptchaUtil;
import com.booth.util.JwtUtil;
import com.booth.util.PasswordUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 认证服务：注册 / 登录 / 验证码
 */
@Service
public class AuthService {

    private final MerchantMapper merchantMapper;
    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;

    /** 短信验证码: phone -> code */
    private final ConcurrentHashMap<String, String> smsStore = new ConcurrentHashMap<>();

    /** 登录失败计数: account -> [失败次数, 锁定截止时间] */
    private final ConcurrentHashMap<String, long[]> failStore = new ConcurrentHashMap<>();

    public AuthService(MerchantMapper merchantMapper, AdminMapper adminMapper, JwtUtil jwtUtil) {
        this.merchantMapper = merchantMapper;
        this.adminMapper = adminMapper;
        this.jwtUtil = jwtUtil;
    }

    /** 发送短信验证码（演示环境直接返回验证码） */
    public Map<String, Object> sendSms(String phone) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        smsStore.put(phone, code);
        Map<String, Object> result = new HashMap<>();
        result.put("devCode", code); // 演示：真实环境应由短信服务商下发
        result.put("message", "验证码已发送");
        return result;
    }

    /** 商户注册 */
    public Map<String, Object> register(RegisterRequest req) {
        // 校验两次密码一致
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        // 证件号唯一
        Long licenseCount = merchantMapper.selectCount(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getLicenseNo, req.getLicenseNo().trim()));
        if (licenseCount > 0) {
            throw new BusinessException("该证件号已注册，请直接登录");
        }
        // 手机号唯一
        Long phoneCount = merchantMapper.selectCount(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getPhone, req.getPhone().trim()));
        if (phoneCount > 0) {
            throw new BusinessException("该手机号已绑定其他账号");
        }
        // 短信验证码
        String savedCode = smsStore.get(req.getPhone());
        if (savedCode == null || !savedCode.equals(req.getSmsCode())) {
            throw new BusinessException("验证码错误，请重新获取");
        }
        smsStore.remove(req.getPhone());

        Merchant merchant = new Merchant();
        merchant.setName(req.getName().trim());
        merchant.setCategories(String.join(",", req.getCategories()));
        merchant.setLicenseNo(req.getLicenseNo().trim());
        merchant.setPhone(req.getPhone().trim());
        merchant.setEmail(req.getEmail());
        merchant.setPassword(PasswordUtil.encode(req.getPassword()));
        merchant.setStatus("PENDING"); // 待审核
        merchant.setCreatedAt(LocalDateTime.now());
        merchant.setUpdatedAt(LocalDateTime.now());
        merchantMapper.insert(merchant);

        String token = jwtUtil.generateToken(merchant.getId(), "MERCHANT");
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("merchantId", merchant.getId());
        return result;
    }

    /** 商户登录 */
    public Map<String, Object> login(LoginRequest req) {
        // 图形验证码
        if (!CaptchaUtil.verify(req.getCaptchaId(), req.getCaptchaCode())) {
            throw new BusinessException("验证码错误");
        }
        String account = req.getAccount().trim();
        // 锁定校验
        long[] fail = failStore.get(account);
        if (fail != null && fail[1] > System.currentTimeMillis()) {
            throw new BusinessException("账号已临时锁定，请 15 分钟后重试");
        }
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getLicenseNo, account).or().eq(Merchant::getPhone, account));
        if (merchant == null) {
            throw new BusinessException("账号不存在，请先注册");
        }
        if (!PasswordUtil.matches(req.getPassword(), merchant.getPassword())) {
            recordFail(account);
            throw new BusinessException("密码错误");
        }
        failStore.remove(account);

        String token = jwtUtil.generateToken(merchant.getId(), "MERCHANT");
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("merchantId", merchant.getId());
        result.put("name", merchant.getName());
        return result;
    }

    /** 连续失败 5 次锁定 15 分钟 */
    private void recordFail(String account) {
        long[] fail = failStore.computeIfAbsent(account, k -> new long[]{0, 0});
        fail[0]++;
        if (fail[0] >= 5) {
            fail[1] = System.currentTimeMillis() + 15 * 60 * 1000L;
            fail[0] = 0;
            throw new BusinessException("密码错误，账号已临时锁定，请 15 分钟后重试");
        }
    }

    /** 管理员登录 */
    public Map<String, Object> adminLogin(String username, String password) {
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, username.trim()));
        if (admin == null || !admin.getPassword().equals(PasswordUtil.encode(password))) {
            throw new BusinessException("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(admin.getId(), "ADMIN");
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("adminName", admin.getRealName() == null ? admin.getUsername() : admin.getRealName());
        return result;
    }
}
