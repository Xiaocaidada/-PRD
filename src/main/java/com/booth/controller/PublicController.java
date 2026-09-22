package com.booth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.booth.common.BusinessException;
import com.booth.common.Result;
import com.booth.entity.*;
import com.booth.mapper.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 公开接口（无需登录）：落地页数据
 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final NavItemMapper navItemMapper;
    private final BannerMapper bannerMapper;
    private final NewsMapper newsMapper;
    private final ArticleCategoryMapper categoryMapper;
    private final RepresentativeMapper representativeMapper;
    private final PartnerMapper partnerMapper;
    private final MerchantMapper merchantMapper;
    private final StallMapper stallMapper;

    public PublicController(NavItemMapper navItemMapper, BannerMapper bannerMapper, NewsMapper newsMapper,
                            ArticleCategoryMapper categoryMapper, RepresentativeMapper representativeMapper,
                            PartnerMapper partnerMapper, MerchantMapper merchantMapper, StallMapper stallMapper) {
        this.navItemMapper = navItemMapper;
        this.bannerMapper = bannerMapper;
        this.newsMapper = newsMapper;
        this.categoryMapper = categoryMapper;
        this.representativeMapper = representativeMapper;
        this.partnerMapper = partnerMapper;
        this.merchantMapper = merchantMapper;
        this.stallMapper = stallMapper;
    }

    /** 落地页全部数据 */
    @GetMapping("/landing")
    public Result<Map<String, Object>> landing() {
        // 导航树（两级）
        List<NavItem> navs = navItemMapper.selectList(new LambdaQueryWrapper<NavItem>()
                .orderByAsc(NavItem::getSort));
        List<NavItem> level1 = navs.stream().filter(n -> n.getParentId() == null || n.getParentId() == 0).toList();
        List<Map<String, Object>> navTree = new ArrayList<>();
        for (NavItem l1 : level1) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", l1.getId());
            node.put("name", l1.getName());
            node.put("url", l1.getUrl());
            node.put("children", navs.stream().filter(n -> l1.getId().equals(n.getParentId()))
                    .map(n -> {
                        Map<String, Object> c = new HashMap<>();
                        c.put("id", n.getId());
                        c.put("name", n.getName());
                        c.put("url", n.getUrl());
                        return c;
                    }).collect(Collectors.toList()));
            navTree.add(node);
        }

        // Banner（上架）
        List<Banner> banners = bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, "ON").orderByAsc(Banner::getSort));

        // 重要通知 + 新闻列表
        List<News> important = newsMapper.selectList(new LambdaQueryWrapper<News>()
                .eq(News::getStatus, "PUBLISHED").eq(News::getIsImportant, 1)
                .orderByDesc(News::getCreatedAt).last("limit 5"));
        List<News> newsList = newsMapper.selectList(new LambdaQueryWrapper<News>()
                .eq(News::getStatus, "PUBLISHED")
                .orderByDesc(News::getCreatedAt).last("limit 20"));

        // 固定展示分类
        List<ArticleCategory> categories = categoryMapper.selectList(new LambdaQueryWrapper<ArticleCategory>()
                .eq(ArticleCategory::getFixedOnHome, 1).orderByAsc(ArticleCategory::getId));

        // 代表人物 + 合作单位
        List<Representative> representatives = representativeMapper.selectList(
                new LambdaQueryWrapper<Representative>().eq(Representative::getStatus, "ON")
                        .orderByAsc(Representative::getSort));
        List<Partner> partners = partnerMapper.selectList(
                new LambdaQueryWrapper<Partner>().eq(Partner::getStatus, "ON")
                        .orderByAsc(Partner::getSort));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("navTree", navTree);
        data.put("banners", banners);
        data.put("importantNews", important);
        data.put("newsList", newsList);
        data.put("categories", categories);
        data.put("representatives", representatives);
        data.put("partners", partners);
        return Result.ok(data);
    }

    /** 新闻详情 */
    @GetMapping("/news/{id}")
    public Result<News> newsDetail(@PathVariable Long id) {
        return Result.ok(newsMapper.selectById(id));
    }

    /** 商户收款页公开信息 */
    @GetMapping("/merchant/{id}")
    public Result<Map<String, Object>> merchantPublic(@PathVariable Long id) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw new BusinessException("商户不存在");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", merchant.getName());
        data.put("stallNo", merchant.getAssignedStallId() == null ? null
                : Optional.ofNullable(stallMapper.selectById(merchant.getAssignedStallId()))
                .map(Stall::getStallNo).orElse(null));
        return Result.ok(data);
    }


    /** 市集竞标页 Banner 轮播（上架中，按 sort 升序） */
    @GetMapping("/banners")
    public Result<List<Map<String, Object>>> banners() {
        List<Banner> list = bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, "ON")
                .orderByAsc(Banner::getSort));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Banner b : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", b.getId());
            row.put("title", b.getTitle());
            row.put("imageUrl", b.getImageUrl());
            row.put("linkUrl", b.getLinkUrl());
            rows.add(row);
        }
        return Result.ok(rows);
    }

    /** 市集竞标页滚动快讯（已发布新闻，按时间倒序取前 20 条） */
    @GetMapping("/notices")
    public Result<List<Map<String, Object>>> notices() {
        List<News> list = newsMapper.selectList(new LambdaQueryWrapper<News>()
                .eq(News::getStatus, "PUBLISHED")
                .isNotNull(News::getTitle)
                .ne(News::getTitle, "")
                .orderByDesc(News::getCreatedAt)
                .last("limit 20"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (News n : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", n.getId());
            row.put("title", n.getTitle());
            row.put("linkUrl", n.getLinkUrl());
            rows.add(row);
        }
        return Result.ok(rows);
    }








}
