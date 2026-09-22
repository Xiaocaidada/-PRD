package com.booth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.booth.common.BusinessException;
import com.booth.common.Result;
import com.booth.entity.LayoutTemplate;
import com.booth.entity.Market;
import com.booth.mapper.LayoutTemplateMapper;
import com.booth.mapper.MarketMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 市集布局模板（管理端）
 */
@RestController
@RequestMapping("/api/admin/layout-template")
public class LayoutTemplateController {

    private final LayoutTemplateMapper layoutTemplateMapper;
    private final MarketMapper marketMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LayoutTemplateController(LayoutTemplateMapper layoutTemplateMapper, MarketMapper marketMapper) {
        this.layoutTemplateMapper = layoutTemplateMapper;
        this.marketMapper = marketMapper;
    }

    private void requireAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(request.getAttribute("role"))) {
            throw new BusinessException("无权限：请使用管理员账号登录");
        }
    }

    /** 模板列表（可关键词搜索 + 状态筛选） */
    @GetMapping("/list")
    public Result<List<LayoutTemplate>> list(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String status,
                                             HttpServletRequest request) {
        requireAdmin(request);
        LambdaQueryWrapper<LayoutTemplate> wrapper = new LambdaQueryWrapper<LayoutTemplate>()
                .like(keyword != null && !keyword.isEmpty(), LayoutTemplate::getName, keyword)
                .eq("ENABLED".equals(status), LayoutTemplate::getStatus, "ENABLED")
                .eq("DISABLED".equals(status), LayoutTemplate::getStatus, "DISABLED")
                .orderByDesc(LayoutTemplate::getUpdatedAt);
        List<LayoutTemplate> list = layoutTemplateMapper.selectList(wrapper);
        // 计算每个模板的摊位数量与绑定市集数量
        for (LayoutTemplate t : list) {
            t.setStallCount(countStalls(t.getElements()));
            t.setMarketCount(Math.toIntExact(marketMapper.selectCount(
                    new LambdaQueryWrapper<Market>().eq(Market::getTemplateId, t.getId()))));
        }
        return Result.ok(list);
    }

    /** 模板详情（编辑器加载） */
    @GetMapping("/{id}")
    public Result<LayoutTemplate> detail(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        LayoutTemplate t = layoutTemplateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException("模板不存在");
        }
        t.setStallCount(countStalls(t.getElements()));
        return Result.ok(t);
    }

    /** 新建模板（空白模板，默认启用） */
    @PostMapping("/create")
    public Result<Long> create(@RequestBody LayoutTemplate body, HttpServletRequest request) {
        requireAdmin(request);
        if (body.getName() == null || body.getName().trim().isEmpty()) {
            throw new BusinessException("请输入模板名称");
        }
        LayoutTemplate t = new LayoutTemplate();
        t.setName(body.getName().trim());
        t.setCanvasWidth(body.getCanvasWidth() == null ? 210 : body.getCanvasWidth());
        t.setCanvasHeight(body.getCanvasHeight() == null ? 297 : body.getCanvasHeight());
        t.setGridSize(body.getGridSize() == null ? 5 : body.getGridSize());
        t.setBackground(body.getBackground());
        t.setElements(body.getElements() == null ? "[]" : body.getElements());
        t.setStatus("ENABLED");
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        layoutTemplateMapper.insert(t);
        return Result.ok("创建成功", t.getId());
    }

    /** 保存编辑模板 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody LayoutTemplate body, HttpServletRequest request) {
        requireAdmin(request);
        LayoutTemplate t = layoutTemplateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException("模板不存在");
        }
        if (body.getName() == null || body.getName().trim().isEmpty()) {
            throw new BusinessException("请输入模板名称");
        }
        t.setName(body.getName().trim());
        if (body.getCanvasWidth() != null) t.setCanvasWidth(body.getCanvasWidth());
        if (body.getCanvasHeight() != null) t.setCanvasHeight(body.getCanvasHeight());
        if (body.getGridSize() != null) t.setGridSize(body.getGridSize());
        if (body.getBackground() != null) t.setBackground(body.getBackground());
        if (body.getElements() != null) t.setElements(body.getElements());
        t.setUpdatedAt(LocalDateTime.now());
        layoutTemplateMapper.updateById(t);
        return Result.ok("保存成功", null);
    }

    /** 复制模板（名称追加「 - 副本」，默认启用） */
    @PostMapping("/{id}/duplicate")
    public Result<Long> duplicate(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        LayoutTemplate t = layoutTemplateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException("模板不存在");
        }
        String newName = duplicateName(t.getName());
        LayoutTemplate copy = new LayoutTemplate();
        copy.setName(newName);
        copy.setCanvasWidth(t.getCanvasWidth());
        copy.setCanvasHeight(t.getCanvasHeight());
        copy.setGridSize(t.getGridSize());
        copy.setBackground(t.getBackground());
        copy.setElements(t.getElements());
        copy.setStatus("ENABLED");
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());
        layoutTemplateMapper.insert(copy);
        return Result.ok("复制成功", copy.getId());
    }

    /** 启用/禁用模板 */
    @PostMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id, @RequestBody Map<String, Boolean> body, HttpServletRequest request) {
        requireAdmin(request);
        LayoutTemplate t = layoutTemplateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException("模板不存在");
        }
        boolean enable = body.getOrDefault("enable", false);
        t.setStatus(enable ? "ENABLED" : "DISABLED");
        t.setUpdatedAt(LocalDateTime.now());
        layoutTemplateMapper.updateById(t);
        return Result.ok(enable ? "启用成功" : "禁用成功", null);
    }

    /** 统计 elements 中摊位元素数量（兼容旧版 stall / 新版 squares 方块 booth） */
    private int countStalls(String elements) {
        if (elements == null || elements.isBlank() || "[]".equals(elements.trim())) {
            return 0;
        }
        try {
            List<Map<String, Object>> list = objectMapper.readValue(elements, new TypeReference<List<Map<String, Object>>>() {});
            int count = 0;
            for (Map<String, Object> e : list) {
                String type = e.get("type") == null ? "" : e.get("type").toString();
                if ("stall".equals(type) || "booth".equals(type)) {
                    count++;
                }
            }
            return count;
        } catch (Exception ex) {
            return 0;
        }
    }

    /** 副本命名：xxx - 副本 / xxx - 副本(2) ... */
    private String duplicateName(String base) {
        String prefix = base + " - 副本";
        if (layoutTemplateMapper.selectCount(new LambdaQueryWrapper<LayoutTemplate>().eq(LayoutTemplate::getName, prefix)) == 0) {
            return prefix;
        }
        int n = 2;
        while (layoutTemplateMapper.selectCount(
                new LambdaQueryWrapper<LayoutTemplate>().eq(LayoutTemplate::getName, prefix + "(" + n + ")")) > 0) {
            n++;
        }
        return prefix + "(" + n + ")";
    }
}
