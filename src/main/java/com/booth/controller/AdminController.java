package com.booth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.booth.common.BusinessException;
import com.booth.common.Result;
import com.booth.dto.AssignStallRequest;
import com.booth.dto.ReviewRequest;
import com.booth.entity.*;
import com.booth.mapper.*;
import com.booth.service.MaterialServiceImpl;
import com.booth.service.StallService;
import com.booth.service.StatsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 管理端接口
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Value("${booth.upload-dir}")
    private String uploadDir;

    private final MerchantMapper merchantMapper;
    private final MaterialMapper materialMapper;
    private final StallMapper stallMapper;
    private final StallBidMapper stallBidMapper;
    private final NewsMapper newsMapper;
    private final ArticleCategoryMapper categoryMapper;
    private final NavItemMapper navItemMapper;
    private final BannerMapper bannerMapper;
    private final RepresentativeMapper representativeMapper;
    private final PartnerMapper partnerMapper;
    private final StallAssignLogMapper assignLogMapper;
    private final MaterialServiceImpl materialServiceImpl;
    private final StallService stallService;
    private final StatsService statsService;
    private final MerchantEditMapper merchantEditMapper;

    public AdminController(MerchantMapper merchantMapper, MaterialMapper materialMapper, StallMapper stallMapper,
                           StallBidMapper stallBidMapper,
                           NewsMapper newsMapper, ArticleCategoryMapper categoryMapper, NavItemMapper navItemMapper,
                           BannerMapper bannerMapper, RepresentativeMapper representativeMapper, PartnerMapper partnerMapper,
                           StallAssignLogMapper assignLogMapper, MaterialServiceImpl materialServiceImpl,
                           StallService stallService, StatsService statsService,MerchantEditMapper merchantEditMapper) {
        this.merchantMapper = merchantMapper;
        this.materialMapper = materialMapper;
        this.stallMapper = stallMapper;
        this.stallBidMapper = stallBidMapper;
        this.newsMapper = newsMapper;
        this.categoryMapper = categoryMapper;
        this.navItemMapper = navItemMapper;
        this.bannerMapper = bannerMapper;
        this.representativeMapper = representativeMapper;
        this.partnerMapper = partnerMapper;
        this.assignLogMapper = assignLogMapper;
        this.materialServiceImpl = materialServiceImpl;
        this.stallService = stallService;
        this.statsService = statsService;
        this.merchantEditMapper=merchantEditMapper;
    }

    private void requireAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(request.getAttribute("role"))) {
            throw new BusinessException("无权限：请使用管理员账号登录");
        }
    }

    /* ==================== 5.15 用户列表 ==================== */

    @GetMapping("/users")
    public Result<Page<Merchant>> users(@RequestParam(required = false) String name,
                                        @RequestParam(required = false) String licenseNo,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "20") long size,
                                        HttpServletRequest request) {
        requireAdmin(request);
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
                .like(name != null && !name.isEmpty(), Merchant::getName, name)
                .eq(licenseNo != null && !licenseNo.isEmpty(), Merchant::getLicenseNo, licenseNo)
                .eq(status != null && !status.isEmpty(), Merchant::getStatus, status)
                .orderByDesc(Merchant::getCreatedAt);
        return Result.ok(merchantMapper.selectPage(new Page<>(page, size), wrapper));
    }


    /** 审核用户：通过 */
    @PostMapping("/users/{id}/approve")
    @Transactional
    public Result<Void> approveUser(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        Merchant merchant = merchantMapper.selectById(id);
        MerchantEdit edit=merchantEditMapper.selectOne(new QueryWrapper<MerchantEdit>().eq("merchant_id",id).eq("audit_status",1));
        if (merchant == null) {
            throw new BusinessException("用户不存在");
        }
        if(edit==null){
            throw new BusinessException("编辑对象不存在");
        }
        UpdateWrapper<Merchant> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set(edit.getName()!=null, "name", edit.getName());
        updateWrapper.set(edit.getPhone()!=null, "phone", edit.getPhone());
        updateWrapper.set(edit.getGender()!=null, "gender", edit.getGender());
        updateWrapper.set(edit.getBirthDate()!=null, "birth_date", edit.getBirthDate());
        updateWrapper.set(edit.getBirthPlace()!=null, "birth_place", edit.getBirthPlace());
        updateWrapper.set(edit.getCategories()!=null, "categories", edit.getCategories());
        updateWrapper.set(edit.getLicenseNo()!=null, "license_no", edit.getLicenseNo());
        updateWrapper.set(edit.getLicensePhoto()!=null, "license_photo", edit.getLicensePhoto());
        updateWrapper.set(edit.getEmail()!=null, "email", edit.getEmail());
        updateWrapper.set(edit.getGoodsInfo()!=null,"goods_info", edit.getGoodsInfo());
        updateWrapper.set(edit.getPriceInfo()!=null,"price_info", edit.getPriceInfo());
        updateWrapper.set("status","APPROVED");
        updateWrapper.set("reject_reason",null);
        updateWrapper.set("updated_at",LocalDateTime.now());
        merchantMapper.update(updateWrapper.eq("id",id));
        UpdateWrapper<MerchantEdit> set= new UpdateWrapper<MerchantEdit>().set("audit_status", Integer.valueOf(2)).set("updated_at", LocalDateTime.now())
                .set("audit_time", LocalDateTime.now());
        merchantEditMapper.update(set);
        return Result.ok("已审核通过", null);
    }

    /** 审核用户：驳回 */
    @PostMapping("/users/{id}/reject")
    public Result<Void> rejectUser(@PathVariable Long id, @RequestBody Map<String, String> body,
                                   HttpServletRequest request) {
        requireAdmin(request);
        String reason = body.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("驳回原因不能为空");
        }
        MerchantEdit merchant = merchantEditMapper.selectOne(new LambdaQueryWrapper<MerchantEdit>().eq(MerchantEdit::getMerchantId, id).eq(MerchantEdit::getAuditStatus, 1));
        if (merchant == null) {
            throw new BusinessException("用户不存在");
        }
        merchant.setAuditStatus(3);
        merchant.setAuditAdminId(Long.valueOf(0));
        merchant.setAuditTime(LocalDateTime.now());
        merchant.setRejectReason(reason.trim());
        merchant.setUpdatedAt(LocalDateTime.now());
        merchantEditMapper.updateById(merchant);
        return Result.ok("已驳回", null);
    }

    /* ==================== 5.19 材料审核 ==================== */

    @GetMapping("/materials")
    public Result<List<Material>> materials(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) Long merchantId,
                                            HttpServletRequest request) {
        requireAdmin(request);
        return Result.ok(materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .eq(status != null && !status.isEmpty(), Material::getStatus, status)
                .eq(merchantId != null, Material::getMerchantId, merchantId)
                .orderByAsc(Material::getStatus).orderByDesc(Material::getCreatedAt)));
    }

    @PostMapping("/materials/review")
    public Result<Void> reviewMaterial(@RequestBody ReviewRequest req, HttpServletRequest request) {
        requireAdmin(request);
        materialServiceImpl.review(req.getMaterialId(), Boolean.TRUE.equals(req.getApprove()), req.getRejectReason());
        return Result.ok("操作成功", null);
    }

    /* ==================== 5.20 信息卡编辑 ==================== */

    @PutMapping("/merchants/{id}/card")
    public Result<Void> editCard(@PathVariable Long id, @RequestBody Merchant patch, HttpServletRequest request) {
        requireAdmin(request);
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw new BusinessException("商户不存在");
        }
        // 仅允许编辑信息卡可编辑字段
        if (patch.getName() != null) merchant.setName(patch.getName());
        if (patch.getCategories() != null) merchant.setCategories(patch.getCategories());
        if (patch.getGender() != null) merchant.setGender(patch.getGender());
        if (patch.getBirthDate() != null) merchant.setBirthDate(patch.getBirthDate());
        if (patch.getBirthPlace() != null) merchant.setBirthPlace(patch.getBirthPlace());
        if (patch.getLicensePhoto() != null) merchant.setLicensePhoto(patch.getLicensePhoto());
        if (patch.getGoodsInfo() != null) merchant.setGoodsInfo(patch.getGoodsInfo());
        if (patch.getPriceInfo() != null) merchant.setPriceInfo(patch.getPriceInfo());
        merchant.setUpdatedAt(LocalDateTime.now());
        merchantMapper.updateById(merchant);
        return Result.ok("保存成功，已同步至商户端", null);
    }

    /* ==================== 5.21 摊位分配 ==================== */

    @GetMapping("/stalls/map")
    public Result<List<Map<String, Object>>> stallMap(HttpServletRequest request) {
        requireAdmin(request);
        return Result.ok(stallService.mapData(null));
    }

    /** 全部竞标（按商户分组） */
    @GetMapping("/stalls/bids")
    public Result<Map<Long, List<Long>>> allBids(HttpServletRequest request) {
        requireAdmin(request);
        Map<Long, List<Long>> grouped = new HashMap<>();
        for (StallBid b : stallBidMapper.selectList(null)) {
            grouped.computeIfAbsent(b.getMerchantId(), k -> new ArrayList<>()).add(b.getStallId());
        }
        return Result.ok(grouped);
    }

    @PostMapping("/stalls/assign")
    public Result<Void> assignStall(@RequestBody AssignStallRequest req, HttpServletRequest request) {
        requireAdmin(request);
        String operator = String.valueOf(request.getAttribute("userId")) ;
        stallService.assign(req.getMerchantId(), req.getStallId(), String.valueOf(operator));
        return Result.ok("分配成功，已同步至商户端", null);
    }

    /** 释放摊位 */
    @PostMapping("/stalls/release")
    public Result<Void> releaseStall(@RequestBody Map<String, Long> body, HttpServletRequest request) {
        requireAdmin(request);
        Long merchantId = body.get("merchantId");
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null || merchant.getAssignedStallId() == null) {
            throw new BusinessException("该商户未分配摊位");
        }
        Stall stall = stallMapper.selectById(merchant.getAssignedStallId());
        if (stall != null) {
            LambdaUpdateWrapper<Stall> stallWrapper = Wrappers.lambdaUpdate();
            stallWrapper.eq(Stall::getId,stall.getId())
                    .set(Stall::getStatus, "AVAILABLE")
                    .set(Stall::getOccupantMerchantId, null); // 强制set null
            stallMapper.update(null, stallWrapper);
        }
// 更新商户：assignedStallId置null
        LambdaUpdateWrapper<Merchant> merchantWrapper = Wrappers.lambdaUpdate();
        merchantWrapper.eq(Merchant::getId, merchantId)
                .set(Merchant::getAssignedStallId, null); //强制set null
        merchantMapper.update(null, merchantWrapper);
        return Result.ok("已释放", null);
    }

    @GetMapping("/assign-logs")
    public Result<List<StallAssignLog>> assignLogs(HttpServletRequest request) {
        requireAdmin(request);
        return Result.ok(assignLogMapper.selectList(new LambdaQueryWrapper<StallAssignLog>()
                .orderByDesc(StallAssignLog::getCreatedAt).last("limit 50")));
    }

    /* ==================== 5.22 新闻中心-活动发布 ==================== */

    @GetMapping("/news")
    public Result<List<News>> newsList(@RequestParam(required = false) String status,
                                       @RequestParam(required = false) Long categoryId,
                                       HttpServletRequest request) {
        requireAdmin(request);
        return Result.ok(newsMapper.selectList(new LambdaQueryWrapper<News>()
                .eq(status != null && !status.isEmpty(), News::getStatus, status)
                .eq(categoryId != null, News::getCategoryId, categoryId)
                .orderByDesc(News::getCreatedAt)));
    }

    @PostMapping("/news")
    public Result<Long> createNews(@RequestBody News news, HttpServletRequest request) {
        requireAdmin(request);
        news.setId(null);
        news.setStatus(news.getStatus() == null ? "PUBLISHED" : news.getStatus());
        news.setCreatedAt(LocalDateTime.now());
        news.setUpdatedAt(LocalDateTime.now());
        newsMapper.insert(news);
        return Result.ok(news.getId());
    }

    @PutMapping("/news/{id}")
    public Result<Void> updateNews(@PathVariable Long id, @RequestBody News patch, HttpServletRequest request) {
        requireAdmin(request);
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new BusinessException("活动不存在");
        }
        if (patch.getTitle() != null) news.setTitle(patch.getTitle());
        if (patch.getCategoryId() != null) news.setCategoryId(patch.getCategoryId());
        if (patch.getSummary() != null) news.setSummary(patch.getSummary());
        if (patch.getContent() != null) news.setContent(patch.getContent());
        if (patch.getCoverUrl() != null) news.setCoverUrl(patch.getCoverUrl());
        if (patch.getIsImportant() != null) news.setIsImportant(patch.getIsImportant());
        if (patch.getActivityTime() != null) news.setActivityTime(patch.getActivityTime());
        if (patch.getActivityLocation() != null) news.setActivityLocation(patch.getActivityLocation());
        if (patch.getRegisterDeadline() != null) news.setRegisterDeadline(patch.getRegisterDeadline());
        news.setUpdatedAt(LocalDateTime.now());
        newsMapper.updateById(news);
        return Result.ok("保存成功，已同步至商户端", null);
    }

    /** 下线 */
    @PostMapping("/news/{id}/offline")
    public Result<Void> offlineNews(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new BusinessException("活动不存在");
        }
        news.setStatus("OFFLINE");
        newsMapper.updateById(news);
        return Result.ok("已下线", null);
    }

    /** 重新上架 */
    @PostMapping("/news/{id}/online")
    public Result<Void> onlineNews(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new BusinessException("活动不存在");
        }
        news.setStatus("PUBLISHED");
        newsMapper.updateById(news);
        return Result.ok("已上架", null);
    }

    /** 删除（仅已下线可删） */
    @DeleteMapping("/news/{id}")
    public Result<Void> deleteNews(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new BusinessException("活动不存在");
        }
        if (!"OFFLINE".equals(news.getStatus())) {
            throw new BusinessException("仅'已下线'状态的活动可删除");
        }
        newsMapper.deleteById(id);
        return Result.ok("已删除", null);
    }

    /* ==================== 5.18 文章分类管理 ==================== */

    @GetMapping("/categories")
    public Result<List<ArticleCategory>> categories(HttpServletRequest request) {
        requireAdmin(request);
        return Result.ok(categoryMapper.selectList(new LambdaQueryWrapper<ArticleCategory>()
                .orderByAsc(ArticleCategory::getId)));
    }

    @PostMapping("/categories")
    public Result<Long> createCategory(@RequestBody ArticleCategory category, HttpServletRequest request) {
        requireAdmin(request);
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new BusinessException("分类名称不能为空");
        }
        category.setId(null);
        category.setCreatedAt(LocalDateTime.now());
        categoryMapper.insert(category);
        // 自动生成分类页 URL
        category.setUrl("/category/" + category.getId());
        categoryMapper.updateById(category);
        return Result.ok(category.getId());
    }

    @PutMapping("/categories/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody ArticleCategory patch,
                                       HttpServletRequest request) {
        requireAdmin(request);
        ArticleCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        if (patch.getName() != null) category.setName(patch.getName());
        if (patch.getFixedOnHome() != null) category.setFixedOnHome(patch.getFixedOnHome());
        categoryMapper.updateById(category);
        return Result.ok();
    }

    /** 删除分类：不影响已有文章，文章归属分类置空 */
    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        List<News> news = newsMapper.selectList(new LambdaQueryWrapper<News>().eq(News::getCategoryId, id));
        for (News n : news) {
            n.setCategoryId(null);
            newsMapper.updateById(n);
        }
        categoryMapper.deleteById(id);
        return Result.ok();
    }

    /* ==================== 5.13 导航管理 ==================== */

    @GetMapping("/navs")
    public Result<List<NavItem>> navs(HttpServletRequest request) {
        requireAdmin(request);
        return Result.ok(navItemMapper.selectList(new LambdaQueryWrapper<NavItem>()
                .orderByAsc(NavItem::getSort).orderByAsc(NavItem::getId)));
    }

    @PostMapping("/navs")
    public Result<Long> createNav(@RequestBody NavItem nav, HttpServletRequest request) {
        requireAdmin(request);
        if (nav.getName() == null || nav.getName().trim().isEmpty()) {
            throw new BusinessException("栏目名称不能为空");
        }
        if (nav.getParentId() == null) {
            nav.setParentId(0L);
        }
        if (nav.getParentId() == 0 && nav.getName().length() > 10) {
            throw new BusinessException("一级栏目名限 10 字符");
        }
        nav.setId(null);
        nav.setCreatedAt(LocalDateTime.now());
        navItemMapper.insert(nav);
        return Result.ok(nav.getId());
    }

    @PutMapping("/navs/{id}")
    public Result<Void> updateNav(@PathVariable Long id, @RequestBody NavItem patch, HttpServletRequest request) {
        requireAdmin(request);
        NavItem nav = navItemMapper.selectById(id);
        if (nav == null) {
            throw new BusinessException("栏目不存在");
        }
        if (patch.getName() != null) nav.setName(patch.getName());
        if (patch.getSort() != null) nav.setSort(patch.getSort());
        if (patch.getUrl() != null) nav.setUrl(patch.getUrl());
        navItemMapper.updateById(nav);
        return Result.ok();
    }

    /** 删除（删除父级同步删除子级） */
    @DeleteMapping("/navs/{id}")
    public Result<Void> deleteNav(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        navItemMapper.delete(new LambdaQueryWrapper<NavItem>().eq(NavItem::getParentId, id));
        navItemMapper.deleteById(id);
        return Result.ok("已删除（含子级栏目）", null);
    }

    /* ==================== 5.14 Banner 管理 ==================== */

    @GetMapping("/banners")
    public Result<List<Banner>> banners(HttpServletRequest request) {
        requireAdmin(request);
        return Result.ok(bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .orderByAsc(Banner::getSort)));
    }

    @PostMapping("/banners")
    public Result<Long> createBanner(@RequestBody Banner banner, HttpServletRequest request) {
        requireAdmin(request);
        if (banner.getImageUrl() == null || banner.getImageUrl().isEmpty()) {
            throw new BusinessException("请上传 3:1 图片");
        }
        banner.setId(null);
        banner.setStatus(banner.getStatus() == null ? "ON" : banner.getStatus());
        banner.setCreatedAt(LocalDateTime.now());
        bannerMapper.insert(banner);
        return Result.ok(banner.getId());
    }

    @PutMapping("/banners/{id}")
    public Result<Void> updateBanner(@PathVariable Long id, @RequestBody Banner patch, HttpServletRequest request) {
        requireAdmin(request);
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException("Banner 不存在");
        }
        if (patch.getTitle() != null) banner.setTitle(patch.getTitle());
        if (patch.getImageUrl() != null) banner.setImageUrl(patch.getImageUrl());
        if (patch.getLinkUrl() != null) banner.setLinkUrl(patch.getLinkUrl());
        if (patch.getSort() != null) banner.setSort(patch.getSort());
        if (patch.getStatus() != null) banner.setStatus(patch.getStatus());
        bannerMapper.updateById(banner);
        return Result.ok();
    }

    @DeleteMapping("/banners/{id}")
    public Result<Void> deleteBanner(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        bannerMapper.deleteById(id);
        return Result.ok();
    }

    /* ==================== 5.16 代表人物管理 ==================== */

    @GetMapping("/representatives")
    public Result<List<Representative>> representatives(HttpServletRequest request) {
        requireAdmin(request);
        return Result.ok(representativeMapper.selectList(new LambdaQueryWrapper<Representative>()
                .orderByAsc(Representative::getSort)));
    }

    @PostMapping("/representatives")
    public Result<Long> createRepresentative(@RequestBody Representative rep, HttpServletRequest request) {
        requireAdmin(request);
        if (rep.getName() == null || rep.getName().trim().isEmpty()) {
            throw new BusinessException("姓名不能为空");
        }
        rep.setId(null);
        rep.setStatus(rep.getStatus() == null ? "ON" : rep.getStatus());
        rep.setCreatedAt(LocalDateTime.now());
        representativeMapper.insert(rep);
        return Result.ok(rep.getId());
    }

    @PutMapping("/representatives/{id}")
    public Result<Void> updateRepresentative(@PathVariable Long id, @RequestBody Representative patch,
                                             HttpServletRequest request) {
        requireAdmin(request);
        Representative rep = representativeMapper.selectById(id);
        if (rep == null) {
            throw new BusinessException("代表人物不存在");
        }
        if (patch.getName() != null) rep.setName(patch.getName());
        if (patch.getTitle() != null) rep.setTitle(patch.getTitle());
        if (patch.getAvatar() != null) rep.setAvatar(patch.getAvatar());
        if (patch.getSort() != null) rep.setSort(patch.getSort());
        if (patch.getStatus() != null) rep.setStatus(patch.getStatus());
        representativeMapper.updateById(rep);
        return Result.ok();
    }

    @DeleteMapping("/representatives/{id}")
    public Result<Void> deleteRepresentative(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        representativeMapper.deleteById(id);
        return Result.ok();
    }

    /* ==================== 5.17 合作单位管理 ==================== */

    @GetMapping("/partners")
    public Result<List<Partner>> partners(HttpServletRequest request) {
        requireAdmin(request);
        return Result.ok(partnerMapper.selectList(new LambdaQueryWrapper<Partner>()
                .orderByAsc(Partner::getSort)));
    }

    @PostMapping("/partners")
    public Result<Long> createPartner(@RequestBody Partner partner, HttpServletRequest request) {
        requireAdmin(request);
        if (partner.getName() == null || partner.getName().trim().isEmpty()) {
            throw new BusinessException("单位名称不能为空");
        }
        if (partner.getCategory() == null || partner.getCategory().trim().isEmpty()) {
            throw new BusinessException("所属分类不能为空");
        }
        partner.setId(null);
        partner.setStatus(partner.getStatus() == null ? "ON" : partner.getStatus());
        partner.setCreatedAt(LocalDateTime.now());
        partnerMapper.insert(partner);
        return Result.ok(partner.getId());
    }

    @PutMapping("/partners/{id}")
    public Result<Void> updatePartner(@PathVariable Long id, @RequestBody Partner patch, HttpServletRequest request) {
        requireAdmin(request);
        Partner partner = partnerMapper.selectById(id);
        if (partner == null) {
            throw new BusinessException("合作单位不存在");
        }
        if (patch.getName() != null) partner.setName(patch.getName());
        if (patch.getCategory() != null) partner.setCategory(patch.getCategory());
        if (patch.getSort() != null) partner.setSort(patch.getSort());
        if (patch.getLinkUrl() != null) partner.setLinkUrl(patch.getLinkUrl());
        if (patch.getStatus() != null) partner.setStatus(patch.getStatus());
        partnerMapper.updateById(partner);
        return Result.ok();
    }

    @DeleteMapping("/partners/{id}")
    public Result<Void> deletePartner(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        partnerMapper.deleteById(id);
        return Result.ok();
    }

    /* ==================== 5.23 统计管理 ==================== */

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(HttpServletRequest request) {
        requireAdmin(request);
        return Result.ok(statsService.adminStats());
    }

    @GetMapping("/stats/orders.csv")
    public void exportCsv(HttpServletRequest request, HttpServletResponse response) throws IOException {
        requireAdmin(request);
        String csv = statsService.ordersCsv();
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=orders.csv");
        response.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
    }

    /* ==================== 通用图片上传（Banner/封面/头像） ==================== */

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                              HttpServletRequest request) throws IOException {
        requireAdmin(request);
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "png";
        if (!List.of("png", "jpg", "jpeg", "gif", "webp").contains(ext)) {
            throw new BusinessException("仅支持图片格式");
        }
        File dir = new File(uploadDir, "images");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String stored = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
        file.transferTo(new File(dir, stored));
        Map<String, String> data = new HashMap<>();
        data.put("url", "/uploads/images/" + stored);
        return Result.ok(data);
    }


    @GetMapping("/get_merchant_edit")
    public Result<MerchantEdit> getEdit(@RequestParam("id")Long id){
        MerchantEdit merchantEdit = merchantEditMapper.selectOne(new QueryWrapper<MerchantEdit>().eq("merchant_id", id).eq("audit_status",1));
        return Result.ok(merchantEdit);
    }


    @GetMapping("/all_status")
    public Result<Map<Long,List<String>>> listStatus(){
        List<Material> merchants = materialMapper.selectList(new LambdaQueryWrapper<>());

        Map<Long,List<String>> map=new HashMap<>();
        for (Material material : merchants) {
            Long id=material.getMerchantId();
            String status=material.getStatus();
            if (map.containsKey(id)) map.get(id).add(status);
            else{
                List<String> list=new ArrayList<>();
                list.add(status);
                map.put(id,list);
            }
        }
        return Result.ok(map);
    }

}
