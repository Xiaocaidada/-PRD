package com.booth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.booth.common.BusinessException;
import com.booth.common.Result;
import com.booth.entity.*;
import com.booth.mapper.MerchantEditMapper;
import com.booth.mapper.MerchantMapper;
import com.booth.mapper.NewsMapper;
import com.booth.mapper.StallMapper;
import com.booth.service.MaterialServiceImpl;
import com.booth.service.StallService;
import com.booth.service.StatsService;
import com.booth.util.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 商户端接口
 */
@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    private static final List<String> ALLOWED_EXT = List.of("pdf", "jpg", "jpeg", "png", "doc", "docx");

    @Value("${booth.upload-dir}")
    private String uploadDir;

    private final MerchantMapper merchantMapper;
    private final NewsMapper newsMapper;
    private final StallMapper stallMapper;
    private final MaterialServiceImpl materialServiceImpl;
    private final StallService stallService;
    private final StatsService statsService;
    private final MerchantEditMapper merchantEditMapper;

    public MerchantController(MerchantMapper merchantMapper, NewsMapper newsMapper, StallMapper stallMapper,
                              MaterialServiceImpl materialServiceImpl, StallService stallService, StatsService statsService, MerchantEditMapper merchantEditMapper) {
        this.merchantMapper = merchantMapper;
        this.newsMapper = newsMapper;
        this.stallMapper = stallMapper;
        this.materialServiceImpl = materialServiceImpl;
        this.stallService = stallService;
        this.statsService = statsService;
        this.merchantEditMapper = merchantEditMapper;
    }

    private Long currentMerchantId(HttpServletRequest request) {
        if (!"MERCHANT".equals(request.getAttribute("role"))) {
            throw new BusinessException("无权限：请使用商户账号登录");
        }
        return (Long) request.getAttribute("userId");
    }

    /** 商户摊位信息卡 */
    @GetMapping("/card")
    public Result<Map<String, Object>> card(HttpServletRequest request) {
        Long merchantId = currentMerchantId(request);
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商户不存在");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("merchant", merchant);

        Map<String, List<Material>> approved = new LinkedHashMap<>();
        approved.put("SOURCE", materialServiceImpl.approvedByModule(merchantId, "SOURCE"));
        approved.put("CULTURE", materialServiceImpl.approvedByModule(merchantId, "CULTURE"));
        approved.put("STANDARD", materialServiceImpl.approvedByModule(merchantId, "STANDARD"));
        data.put("approvedMaterials", approved);

        // 已分配摊位
        if (merchant.getAssignedStallId() != null) {
            data.put("assignedStall", stallMapper.selectById(merchant.getAssignedStallId()));
        } else {
            data.put("assignedStall", null);
        }
        Result<Map<String,Object>> res= Result.ok(data);
        System.out.println(res);
        return res;
    }

    /** 我的材料列表 */
    @GetMapping("/materials")
    public Result<List<Material>> materials(@RequestParam("merchantId") Long merchantId,@RequestParam("marketId")Long marketId,HttpServletRequest request) {
        return Result.ok(materialServiceImpl.listByMerchant(merchantId,marketId));
    }



    /** 摊位平面图 + 我的竞标 */
    @GetMapping("/stall-map")
    public Result<List<Map<String, Object>>> stallMap(HttpServletRequest request) {
        Long merchantId = currentMerchantId(request);
        Result<List<Map<String, Object>>> ok = Result.ok(stallService.mapData(merchantId));
        System.out.println(ok);
        return ok;
    }

    /** 保存竞标摊位（同级别可多选） */
    @PostMapping("/bids")
    public Result<Void> saveBids(@RequestBody Map<String, List<Long>> body, HttpServletRequest request) {
        Long merchantId = currentMerchantId(request);
        stallService.saveBids(merchantId, body.getOrDefault("stallIds", List.of()));
        return Result.ok();
    }

    /** 收支统计 + 客流统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(HttpServletRequest request) {
        Long merchantId = currentMerchantId(request);
        return Result.ok(statsService.merchantStats(merchantId));
    }

    /** 付款链接（本期不做真实支付，生成收款页链接） */
    @GetMapping("/pay-link")
    public Result<Map<String, Object>> payLink(HttpServletRequest request) {
        Long merchantId = currentMerchantId(request);
        Merchant merchant = merchantMapper.selectById(merchantId);
        Map<String, Object> data = new HashMap<>();
        data.put("merchantId", merchantId);
        data.put("merchantName", merchant.getName());
        data.put("stallNo", merchant.getAssignedStallId() == null ? null
                : Optional.ofNullable(stallMapper.selectById(merchant.getAssignedStallId()))
                .map(Stall::getStallNo).orElse(null));
        data.put("payUrl", "/pay/" + merchantId);
        return Result.ok(data);
    }

    /** 活动通知（已发布新闻） */
    @GetMapping("/news")
    public Result<List<News>> news() {
        List<News> list = newsMapper.selectList(new LambdaQueryWrapper<News>()
                .eq(News::getStatus, "PUBLISHED").orderByDesc(News::getCreatedAt));
        return Result.ok(list);
    }

    /** 商户名（顶部导航展示用） */
    @GetMapping("/profile")
    public Result<Merchant> profile(HttpServletRequest request) {
        Long merchantId = currentMerchantId(request);
        return Result.ok(merchantMapper.selectById(merchantId));
    }
    @PutMapping("/{id}/card")
    public Result updateMerchantInfo(@PathVariable("id") Long id, @RequestBody MerchantEdit edit) {
        edit.setId(null);
        //更新之前先删除旧的未审核通过数据
        merchantEditMapper.delete(new LambdaQueryWrapper<MerchantEdit>().eq(MerchantEdit::getMerchantId, id).eq(MerchantEdit::getAuditStatus, 1));
        edit.setCreatedAt(LocalDateTime.now());
        edit.setUpdatedAt(LocalDateTime.now());
        edit.setMerchantId(id);
        merchantEditMapper.insert(edit);
        return Result.ok();
    }

/**
 * 处理文件上传请求的接口方法
 * @param file 上传的文件，通过MultipartFile接收
 * @return 返回Result对象，包含上传结果信息
 */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("文件上传执行");
        // 调用FileUtils的uploadLocal方法处理文件上传，获取上传后的文件URL
            String url = FileUtils.uploadLocal(file);
            // 使用你Result的ok方法
            return Result.ok(url);
        } catch (IOException e) {
            e.printStackTrace();
            // 失败返回code=1，message为异常信息
            return Result.fail("文件上传失败：" + e.getMessage());
        }
    }


}
