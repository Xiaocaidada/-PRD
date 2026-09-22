package com.booth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.booth.common.BusinessException;
import com.booth.common.Result;
import com.booth.dto.AssignDTO;
import com.booth.dto.AssignedMerchantDTO;
import com.booth.dto.CandidateMerchantDTO;
import com.booth.dto.Reject;
import com.booth.entity.LayoutTemplate;
import com.booth.entity.Market;
import com.booth.entity.MarketBid;
import com.booth.entity.Material;
import com.booth.mapper.*;
import com.booth.service.MaterialService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 市集管理（管理端）
 */
@RestController
@RequestMapping("/api/admin/market")
public class MarketController {

    private final MarketMapper marketMapper;
    private final LayoutTemplateMapper layoutTemplateMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MerchantMapper merchantMapper;
    private final MarketBidMapper marketBidMapper;
    private final MaterialMapper materialMapper;

    public MarketController(MarketMapper marketMapper, LayoutTemplateMapper layoutTemplateMapper, MerchantMapper merchantMapper, MarketBidMapper marketBidMapper,MaterialMapper materialMapper) {
        this.materialMapper=materialMapper;
        this.marketMapper = marketMapper;
        this.layoutTemplateMapper = layoutTemplateMapper;
        this.merchantMapper = merchantMapper;
        this.marketBidMapper = marketBidMapper;
    }

    private void requireAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(request.getAttribute("role"))) {
            throw new BusinessException("无权限：请使用管理员账号登录");
        }
    }

    /** 市集列表（分页 + 搜索 + 状态筛选） */
    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long size,
                                                  HttpServletRequest request) {
        requireAdmin(request);
        LambdaQueryWrapper<Market> wrapper = new LambdaQueryWrapper<Market>()
                .like(keyword != null && !keyword.isEmpty(), Market::getName, keyword)
                .eq(status != null && !status.isEmpty() && !"ALL".equals(status), Market::getStatus, status)
                .orderByDesc(Market::getCreatedAt);
        Page<Market> p = marketMapper.selectPage(new Page<>(page, size), wrapper);
        Page<Map<String, Object>> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Market m : p.getRecords()) {
            rows.add(decorate(m));
        }
        result.setRecords(rows);
        return Result.ok(result);
    }

    /** 新建市集 */
    @PostMapping("/create")
    public Result<Long> create(@RequestBody Market body, HttpServletRequest request) {
        requireAdmin(request);
        validate(body);
        Market m = new Market();
        fill(m, body);
        m.setStatus("PREPARING");
        m.setBidEnabled(body.getBidEnabled() == null ? 0 : body.getBidEnabled());
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());
        marketMapper.insert(m);
        return Result.ok("创建成功", m.getId());
    }

    /** 市集详情 */
    @GetMapping("/{id}")
    public Result<Market> detail(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        Market m = marketMapper.selectById(id);
        if (m == null) {
            throw new BusinessException("市集不存在");
        }
        return Result.ok(m);
    }

    /** 编辑市集（含绑定/更换布局模板） */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Market body, HttpServletRequest request) {
        requireAdmin(request);
        Market m = marketMapper.selectById(id);
        if (m == null) {
            throw new BusinessException("市集不存在");
        }
        validate(body);
        fill(m, body);
        if (body.getBidEnabled() != null) m.setBidEnabled(body.getBidEnabled());
        m.setUpdatedAt(LocalDateTime.now());
        // 绑定模板变化时刷新布局快照
        if (!Objects.equals(m.getTemplateId(), body.getTemplateId())) {
            m.setTemplateId(body.getTemplateId());
            m.setLayoutSnapshot(snapshotOf(body.getTemplateId()));
        }
        marketMapper.updateById(m);
        return Result.ok("保存成功", null);
    }

    /** 发布市集（筹备中→已发布；已下线→恢复下线前状态） */
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        Market m = marketMapper.selectById(id);
        if (m == null) {
            throw new BusinessException("市集不存在");
        }
        if ("PUBLISHED".equals(m.getStatus())) {
            throw new BusinessException("市集已发布");
        }
        if ("OFFLINE".equals(m.getStatus())) {
            String before = m.getOfflineBeforeStatus();
            m.setStatus(before == null || before.isEmpty() ? "PUBLISHED" : before);
            m.setOfflineBeforeStatus(null);
        } else if ("PREPARING".equals(m.getStatus())) {
            m.setStatus("PUBLISHED");
        } else {
            throw new BusinessException("当前状态不可发布");
        }
        m.setUpdatedAt(LocalDateTime.now());
        marketMapper.updateById(m);
        return Result.ok("发布成功", null);
    }

    /** 下线市集（状态→已下线，竞标开关自动关闭） */
    @PostMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        Market m = marketMapper.selectById(id);
        if (m == null) {
            throw new BusinessException("市集不存在");
        }
        if ("OFFLINE".equals(m.getStatus())) {
            throw new BusinessException("市集已下线");
        }
        m.setOfflineBeforeStatus(m.getStatus());
        m.setStatus("OFFLINE");
        m.setBidEnabled(0);
        m.setUpdatedAt(LocalDateTime.now());
        marketMapper.updateById(m);
        return Result.ok("已下线", null);
    }

    /** 竞标开关 */
    @PostMapping("/{id}/bid-toggle")
    public Result<Void> bidToggle(@PathVariable Long id, @RequestBody Map<String, Integer> body, HttpServletRequest request) {
        requireAdmin(request);
        Market m = marketMapper.selectById(id);
        if (m == null) {
            throw new BusinessException("市集不存在");
        }
        int enabled = body.getOrDefault("bidEnabled", 0);
        m.setBidEnabled(enabled);
        m.setUpdatedAt(LocalDateTime.now());
        marketMapper.updateById(m);
        return Result.ok(enabled == 1 ? "竞标已开启" : "竞标已关闭", null);
    }

    // ==================== 辅助 ====================

    private void validate(Market m) {
        if (m.getName() == null || m.getName().trim().isEmpty()) {
            throw new BusinessException("请填写市集名称");
        }
        if (m.getName().length() > 30) {
            throw new BusinessException("市集名称不能超过30字");
        }
        if (m.getLocation() == null || m.getLocation().trim().isEmpty()) {
            throw new BusinessException("请填写举办地点");
        }
        if (m.getStartDate() == null || m.getEndDate() == null) {
            throw new BusinessException("请填写活动时间");
        }
        if (m.getEndDate().isBefore(m.getStartDate())) {
            throw new BusinessException("结束时间不能早于开始时间");
        }
        if (m.getBidDeadline() != null) {
            LocalDate start = m.getStartDate();
            if (m.getBidDeadline().toLocalDate().isAfter(start)) {
                throw new BusinessException("竞标截止时间须早于活动开始时间");
            }
            if (m.getBidDeadline().isBefore(LocalDateTime.now())) {
                throw new BusinessException("竞标截止时间不能早于当前时间");
            }
        }
    }

    private void fill(Market target, Market body) {
        target.setName(body.getName().trim());
        target.setLocation(body.getLocation().trim());
        target.setStartDate(body.getStartDate());
        target.setEndDate(body.getEndDate());
        target.setBidDeadline(body.getBidDeadline());
        target.setStatement(body.getStatement());
        target.setTemplateId(body.getTemplateId());
        target.setImageUrl(body.getImageUrl());
    }

    /** 生成模板布局快照JSON（含画布尺寸、底图、元素） */
    private String snapshotOf(Long templateId) {
        if (templateId == null) {
            return null;
        }
        LayoutTemplate t = layoutTemplateMapper.selectById(templateId);
        if (t == null) {
            throw new BusinessException("布局模板不存在");
        }
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("canvasWidth", t.getCanvasWidth());
        snap.put("canvasHeight", t.getCanvasHeight());
        snap.put("gridSize", t.getGridSize());
        snap.put("background", t.getBackground());
        snap.put("elements", parseElements(t.getElements()));
        try {
            return objectMapper.writeValueAsString(snap);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> parseElements(String elements) {
        if (elements == null || elements.isBlank() || "[]".equals(elements.trim())) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(elements, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private int countStalls(String elements) {
        int count = 0;
        for (Map<String, Object> e : parseElements(elements)) {
            if (isStallType(e.get("type"))) {
                count++;
            }
        }
        return count;
    }

    /** 判断是否为摊位类型（旧版 stall / 新版 squares 方块 booth） */
    private boolean isStallType(Object type) {
        return type != null && ("stall".equals(type.toString()) || "booth".equals(type.toString()));
    }

    /** 补充模板名/摊位数/竞标状态等展示字段 */
    private Map<String, Object> decorate(Market m) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", m.getId());
        row.put("name", m.getName());
        row.put("location", m.getLocation());
        row.put("startDate", m.getStartDate());
        row.put("endDate", m.getEndDate());
        row.put("bidDeadline", m.getBidDeadline());
        row.put("statement", m.getStatement());
        row.put("templateId", m.getTemplateId());
        row.put("bidEnabled", m.getBidEnabled());
        row.put("status", m.getStatus());
        row.put("createdAt", m.getCreatedAt());
        // 模板信息
        String templateName = null;
        String templateStatus = null;
        int stallCount = 0;
        if (m.getTemplateId() != null) {
            LayoutTemplate t = layoutTemplateMapper.selectById(m.getTemplateId());
            if (t != null) {
                templateName = t.getName();
                templateStatus = t.getStatus();
            }
        }
        if (m.getLayoutSnapshot() != null && !m.getLayoutSnapshot().isBlank()) {
            try {
                Map<String, Object> snap = objectMapper.readValue(m.getLayoutSnapshot(), new TypeReference<Map<String, Object>>() {});
                Object els = snap.get("elements");
                if (els instanceof List) {
                    for (Object o : (List<?>) els) {
                        if (o instanceof Map && isStallType(((Map<?, ?>) o).get("type"))) {
                            stallCount++;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        } else if (m.getTemplateId() != null) {
            LayoutTemplate t = layoutTemplateMapper.selectById(m.getTemplateId());
            if (t != null) {
                stallCount = countStalls(t.getElements());
            }
        }
        row.put("templateName", templateName);
        row.put("templateStatus", templateStatus);
        row.put("stallCount", m.getTemplateId() == null ? null : stallCount);
        // 竞标状态
        String bidState = "OFF";
        if (m.getBidEnabled() != null && m.getBidEnabled() == 1) {
            if (m.getBidDeadline() == null || m.getBidDeadline().isAfter(LocalDateTime.now())) {
                bidState = "OPEN";
            } else {
                bidState = "ENDED";
            }
        }
        row.put("bidState", bidState);
        return row;
    }

    @GetMapping("/candidate/{id}")
    public Result<Object> getMarketCandidate(@PathVariable("id") Long id) {
        List<CandidateMerchantDTO> dataList = merchantMapper.selectCandidateMerchantList(id);
        Map<Long, List<CandidateMerchantDTO>> collect = dataList.stream().collect(Collectors.groupingBy(CandidateMerchantDTO::getId));
        System.out.println(collect);
        return Result.ok(collect);
    }

    @GetMapping("/assigned/{id}")
    public Result<Object> getAssignedMerchant(@PathVariable("id") Long id) {
        List<AssignedMerchantDTO> dataList = merchantMapper.selectAssignedMerchantList(id);
        Map<Long, List<AssignedMerchantDTO>> collect = dataList.stream().collect(Collectors.groupingBy(AssignedMerchantDTO::getId));
        return Result.ok(collect);
    }

    @GetMapping("/stat/{id}")
    public Result getStall(@PathVariable("id") Long id){
        List<MarketBid> marketBids = marketBidMapper.selectList(new LambdaQueryWrapper<MarketBid>().eq(MarketBid::getMarketId, id));
        Set<String> taken=new HashSet<>();
        Set<String> candidate=new HashSet<>();
        for (MarketBid b : marketBids){
            String status=b.getStatus();
            if(!status.isEmpty() && status.equals("PENDING")&&!candidate.contains(status)) candidate.add(b.getStallNo());
            if(!status.isEmpty() && status.equals("APPROVED")&&!taken.contains(status)) taken.add(b.getStallNo());
        }
        Map<String, Set<String>> result = Map.of("taken", taken, "candidate", candidate);
        System.out.println(result);
        return Result.ok(result);
    }

    @PostMapping("/stalls/assign")
    @Transactional
    public Result assign(@RequestBody AssignDTO dto){
        marketBidMapper.delete(new QueryWrapper<MarketBid>()
                .eq("market_id",dto.getMarketId())
                .eq("merchant_id",dto.getMerchantId()));
        MarketBid marketBid = new MarketBid();
        marketBid.setMarketId(dto.getMarketId());
        marketBid.setMerchantId(dto.getMerchantId());
        marketBid.setStallNo(dto.getStallNo());
        marketBid.setStatus("APPROVED");
        marketBid.setCreatedAt(LocalDateTime.now());
        marketBidMapper.insert(marketBid);
        materialMapper.delete(new UpdateWrapper<Material>()
                .eq("market_id",dto.getMarketId())
                .eq("merchant_id",dto.getMerchantId())
                .eq("status","APPROVED"));
        materialMapper.update(new UpdateWrapper<Material>()
                .eq("market_id",dto.getMarketId())
                .eq("merchant_id",dto.getMerchantId())
                .eq("status","PENDING")
                .set("status","APPROVED"));
        return Result.ok();
    }

    @Transactional
    @PostMapping("/release")
    public Result release(@RequestBody Reject reject){
        marketBidMapper.update(new UpdateWrapper<MarketBid>()
                .eq("market_id",reject.getMarketId())
                .eq("stall_no",reject.getStallNo())
                .set("status","REJECTED")
                .set("remark",reject.getReason()));
        materialMapper.update(new UpdateWrapper<Material>()
                .eq("market_id",reject.getMarketId())
                .eq("merchant_id",reject.getMerchantId())
                .eq("status","APPROVED")
                .set("status","REJECTED"));
        return Result.ok();
    }


}
