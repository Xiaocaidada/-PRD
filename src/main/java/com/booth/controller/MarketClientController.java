package com.booth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.booth.common.BusinessException;
import com.booth.common.Result;
import com.booth.dto.BidMaterialDTO;
import com.booth.dto.BidSubmitDTO;
import com.booth.entity.*;
import com.booth.mapper.*;
import com.booth.service.MaterialService;
import com.booth.util.FileUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 市集竞标（商户端）
 */
@RestController
@RequestMapping("/api/merchant/market")
public class MarketClientController {

    private final MarketMapper marketMapper;
    private final LayoutTemplateMapper layoutTemplateMapper;
    private final MarketBidMapper marketBidMapper;
    private final MerchantMapper merchantMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MaterialService materialService;

    public MarketClientController(MarketMapper marketMapper, LayoutTemplateMapper layoutTemplateMapper,
                                  MarketBidMapper marketBidMapper, MerchantMapper merchantMapper,MaterialMapper materialMapper,MaterialService materialService) {
        this.marketMapper = marketMapper;
        this.layoutTemplateMapper = layoutTemplateMapper;
        this.marketBidMapper = marketBidMapper;
        this.merchantMapper = merchantMapper;
        this.materialService = materialService;


    }

    private Long currentMerchantId(HttpServletRequest request) {
        if (!"MERCHANT".equals(request.getAttribute("role"))) {
            throw new BusinessException("无权限：请使用商户账号登录");
        }
        return (Long) request.getAttribute("userId");
    }

    /** 场次列表（已发布的市集；已下线不展示） */
    @GetMapping("/markets")
    public Result<List<Map<String, Object>>> markets(HttpServletRequest request) {
        currentMerchantId(request);
        List<Market> list = marketMapper.selectList(new LambdaQueryWrapper<Market>()
                .eq(Market::getStatus, "PUBLISHED")
                .orderByDesc(Market::getStartDate));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Market m : list) {
            rows.add(decorate(m));
        }
        return Result.ok(rows);
    }

    /** 场次详情（含布局与摊位竞标状态） */
    @GetMapping("/markets/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id, HttpServletRequest request) {
        // 从请求中获取当前商家ID
        Long merchantId = currentMerchantId(request);
        // 根据ID查询市集信息
        Market m = marketMapper.selectById(id);
        // 判断市集是否存在
        if (m == null) {
            throw new BusinessException("市集不存在");
        }
        // 判断市集是否已下线
        if ("OFFLINE".equals(m.getStatus())) {
            throw new BusinessException("该市集已下线");
        }
        // 装饰市集数据
        Map<String, Object> data = decorate(m);
        // 布局数据：优先使用绑定快照，其次取模板当前布局
        Map<String, Object> layout = layoutOf(m);
        data.put("layout", layout);
        // 已被竞标的摊位（非驳回）
        List<MarketBid> takenBids = marketBidMapper.selectList(new LambdaQueryWrapper<MarketBid>()
                .eq(MarketBid::getMarketId, id)
                .eq(MarketBid::getStatus, "APPROVED"));
        Set<String> takenStalls = new HashSet<>();
        for (MarketBid b : takenBids) {
            takenStalls.add(b.getStallNo());
        }
        data.put("takenStalls", takenStalls);
        // 我的竞标记录
        List<MarketBid> myBids = marketBidMapper.selectList(new LambdaQueryWrapper<MarketBid>()
                .eq(MarketBid::getMarketId, id)
                .eq(MarketBid::getMerchantId, merchantId)
                        .eq(MarketBid::getStatus,"APPROVED")
                .orderByDesc(MarketBid::getCreatedAt));
        data.put("myBids", myBids);

        //当前正在竞选的摊位
        List<MarketBid> pending = marketBidMapper.selectList(new LambdaQueryWrapper<MarketBid>()
                .eq(MarketBid::getMarketId, id)
                .eq(MarketBid::getMerchantId, merchantId)
                .eq(MarketBid::getStatus, "PENDING"));
        data.put("myPending", pending);

        return Result.ok(data);
    }

    /** 提交竞标（多选摊位 + 备注） */
    @PostMapping("/markets/{id}/bids")
    @Transactional
    public Result<Object> submitBids(@PathVariable("id") Long id, @RequestBody BidSubmitDTO form,HttpServletRequest request) throws IOException {
        Long merchantId = currentMerchantId(request);
        Market m = marketMapper.selectById(id);
        marketBidMapper.delete(new LambdaQueryWrapper<MarketBid>().eq(MarketBid::getMarketId, id)
                .eq(MarketBid::getMerchantId, merchantId)
                .eq(MarketBid::getStatus,"PENDING"));
        List<String> stalls=form.getStallNos();
        String remark= form.getRemark();
        if (m == null) {
            throw new BusinessException("市集不存在");
        }
        if ("OFFLINE".equals(m.getStatus())) {
            throw new BusinessException("该市集已下线，无法竞标");
        }
        if (!"PUBLISHED".equals(m.getStatus())) {
            throw new BusinessException("该市集暂未开放竞标");
        }
        if (m.getBidEnabled() == null || m.getBidEnabled() != 1) {
            throw new BusinessException("本期竞标暂未开放");
        }
        if (m.getBidDeadline() != null && m.getBidDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("本期竞标已结束");
        }
        if (!(stalls instanceof List) || ((List<?>) stalls).isEmpty()) {
            throw new BusinessException("请选择目标摊位");
        }
        @SuppressWarnings("unchecked")
        List<String> stallNos = (List<String>) stalls;
        if (remark != null && remark.length() > 200) {
            throw new BusinessException("竞标备注不能超过200字");
        }
        // 校验摊位冲突
        List<MarketBid> takenBids = marketBidMapper.selectList(new LambdaQueryWrapper<MarketBid>()
                .eq(MarketBid::getMarketId, id)
                .ne(MarketBid::getStatus, "REJECTED"));
        Set<String> takenStalls = new HashSet<>();
        for (MarketBid b : takenBids) {
            takenStalls.add(b.getStallNo());
        }
        for (String stallNo : stallNos) {
            if (takenStalls.contains(stallNo)) {
                throw new BusinessException("摊位 " + stallNo + " 已被其他商户竞标，请选择其他摊位");
            }
        }
        // 校验目标摊位必须在布局中且为可竞标摊位（booth/旧版 stall），过道/建筑不可竞标
        Set<String> bidableStalls = bidableStallsOf(m);
        for (String stallNo : stallNos) {
            if (!bidableStalls.contains(stallNo)) {
                throw new BusinessException("摊位 " + stallNo + " 不可竞标，请选择布局中的摊位方块");
            }
        }
        for (String stallNo : stallNos) {
            MarketBid bid = new MarketBid();
            bid.setMarketId(id);
            bid.setMerchantId(merchantId);
            bid.setStallNo(stallNo);
            bid.setRemark(remark);
            bid.setStatus("PENDING");
            bid.setCreatedAt(LocalDateTime.now());
            try {
                marketBidMapper.insert(bid);
            } catch (Exception e) {
                // 唯一键冲突：同一商户重复竞标同一摊位
                throw new BusinessException("摊位 " + stallNo + " 重复竞标");
            }
        }
        handlerMaterials(form.getMaterials(),merchantId,id);

        return Result.ok("竞标已提交，等待管理员审核", null);
    }

    private void handlerMaterials(List<BidMaterialDTO> materials, Long merchantId, Long marketId){

        List<Material> materialList = new ArrayList<>();
        for(BidMaterialDTO item : materials){
            if (! material_exists(item)) continue;
            Material material = new Material();
            material.setMarketId(marketId);
            material.setMerchantId(merchantId);
            material.setContent(item.getContent());
            material.setStatus("PENDING");
            material.setModule(item.getModule());
            material.setCreatedAt(LocalDateTime.now());
            material.setRejectReason("");
            material.setFilePath(item.getUrls());
            materialList.add(material);
        }
        materialService.saveList(materialList);

    }

    private boolean material_exists(BidMaterialDTO item) {
        return (item.getUrls()!=null && item.getUrls().size()>0) || (item.getContent().strip().length()>0);

    }


    /** 我的竞标列表 */
    @GetMapping("/my-bids")
    public Result<List<Map<String, Object>>> myBids(HttpServletRequest request) {
        Long merchantId = currentMerchantId(request);
        List<MarketBid> bids = marketBidMapper.selectList(new LambdaQueryWrapper<MarketBid>()
                .eq(MarketBid::getMerchantId, merchantId)
                .orderByDesc(MarketBid::getCreatedAt));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MarketBid b : bids) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", b.getId());
            row.put("stallNo", b.getStallNo());
            row.put("remark", b.getRemark());
            row.put("status", b.getStatus());
            row.put("createdAt", b.getCreatedAt());
            Market m = marketMapper.selectById(b.getMarketId());
            row.put("marketName", m == null ? null : m.getName());
            row.put("marketStatus", m == null ? null : m.getStatus());
            rows.add(row);
        }
        return Result.ok(rows);
    }

    // ==================== 辅助 ====================

    private Map<String, Object> layoutOf(Market m) {
        Map<String, Object> layout = new LinkedHashMap<>();
        if (m.getLayoutSnapshot() != null && !m.getLayoutSnapshot().isBlank()) {
            try {
                return objectMapper.readValue(m.getLayoutSnapshot(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {
            }
        }
        LayoutTemplate t = m.getTemplateId() == null ? null : layoutTemplateMapper.selectById(m.getTemplateId());
        if (t != null) {
            layout.put("canvasWidth", t.getCanvasWidth());
            layout.put("canvasHeight", t.getCanvasHeight());
            layout.put("gridSize", t.getGridSize());
            layout.put("background", t.getBackground());
            layout.put("elements", parseElements(t.getElements()));
            return layout;
        }
        layout.put("canvasWidth", 210);
        layout.put("canvasHeight", 297);
        layout.put("gridSize", 5);
        layout.put("background", null);
        layout.put("elements", new ArrayList<>());
        return layout;
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

    /** 判断是否为可竞标摊位类型（旧版 stall / 新版 squares 方块 booth） */
    private boolean isStallType(Object type) {
        return type != null && ("stall".equals(type.toString()) || "booth".equals(type.toString()));
    }

    /** 提取布局中可竞标摊位的编号集合（新版取 name：A01…；旧版取 stallNo：A1…） */
    private Set<String> bidableStallsOf(Market m) {
        Set<String> set = new HashSet<>();
        Map<String, Object> layout = layoutOf(m);
        Object els = layout.get("elements");
        if (els instanceof List) {
            for (Object o : (List<?>) els) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> e = (Map<?, ?>) o;
                Object type = e.get("type");
                if (!isStallType(type)) continue;
                Object no = e.get("name");
                if (no == null || no.toString().isBlank()) no = e.get("stallNo");
                if (no != null && !no.toString().isBlank()) set.add(no.toString().trim());
            }
        }
        return set;
    }

    private String bidState(Market m) {
        if (m.getBidEnabled() == null || m.getBidEnabled() != 1) {
            return "OFF";
        }
        if (m.getBidDeadline() != null && m.getBidDeadline().isBefore(LocalDateTime.now())) {
            return "ENDED";
        }
        return "OPEN";
    }

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
        row.put("bidState", bidState(m));
        row.put("imageUrl", m.getImageUrl());
        // 布局缩略（供场次卡片渲染）
        Map<String, Object> layout = layoutOf(m);
        row.put("layout", layout);
        Object els = layout.get("elements");
        int total = 0;
        if (els instanceof List) {
            for (Object o : (List<?>) els) {
                if (o instanceof Map && isStallType(((Map<?, ?>) o).get("type"))) {
                    total++;
                }
            }
        }





        // 剩余可竞标 = 总数 - 已被竞标摊位数（去重）
        List<MarketBid> takenBids = marketBidMapper.selectList(new LambdaQueryWrapper<MarketBid>()
                .eq(MarketBid::getMarketId, m.getId())
                .eq(MarketBid::getStatus, "APPROVED"));
        Set<String> takenNos = new HashSet<>();
        for (MarketBid b : takenBids) {
            takenNos.add(b.getStallNo());
        }
        row.put("stallCount", total);
        row.put("remainCount", Math.max(0, total - takenNos.size()));
        return row;
    }
}
