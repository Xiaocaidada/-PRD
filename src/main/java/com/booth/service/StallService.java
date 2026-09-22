package com.booth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.booth.common.BusinessException;
import com.booth.entity.Merchant;
import com.booth.entity.Stall;
import com.booth.entity.StallAssignLog;
import com.booth.entity.StallBid;
import com.booth.mapper.MerchantMapper;
import com.booth.mapper.StallAssignLogMapper;
import com.booth.mapper.StallBidMapper;
import com.booth.mapper.StallMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 摊位服务：竞标 / 分配
 */
@Service
public class StallService {

    private final StallMapper stallMapper;
    private final StallBidMapper stallBidMapper;
    private final MerchantMapper merchantMapper;
    private final StallAssignLogMapper assignLogMapper;

    public StallService(StallMapper stallMapper, StallBidMapper stallBidMapper,
                        MerchantMapper merchantMapper, StallAssignLogMapper assignLogMapper) {
        this.stallMapper = stallMapper;
        this.stallBidMapper = stallBidMapper;
        this.merchantMapper = merchantMapper;
        this.assignLogMapper = assignLogMapper;
    }

    /** 全部摊位 */
    public List<Stall> listAll() {
        return stallMapper.selectList(new LambdaQueryWrapper<Stall>()
                .orderByAsc(Stall::getLevel).orderByAsc(Stall::getStallNo));
    }

    /** 商户当前竞标摊位 ID 集合 */
    public List<Long> myBids(Long merchantId) {
        return stallBidMapper.selectList(new LambdaQueryWrapper<StallBid>()
                        .eq(StallBid::getMerchantId, merchantId))
                .stream().map(StallBid::getStallId).toList();
    }

    /** 保存竞标（全量替换） */
    @Transactional
    public void saveBids(Long merchantId, List<Long> stallIds) {
        stallBidMapper.delete(new LambdaQueryWrapper<StallBid>().eq(StallBid::getMerchantId, merchantId));
        if (stallIds == null || stallIds.isEmpty()) {
            return;
        }
        for (Long stallId : stallIds) {
            Stall stall = stallMapper.selectById(stallId);
            if (stall == null) {
                throw new BusinessException("摊位不存在");
            }
            if ("OCCUPIED".equals(stall.getStatus())) {
                throw new BusinessException("摊位 " + stall.getStallNo() + " 已被占用，不可竞标");
            }
            StallBid bid = new StallBid();
            bid.setMerchantId(merchantId);
            bid.setStallId(stallId);
            bid.setCreatedAt(LocalDateTime.now());
            stallBidMapper.insert(bid);
        }
    }

    /** 管理员分配摊位 */
    @Transactional
    public void assign(Long merchantId, Long stallId, String operator) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商户不存在");
        }
        if (!"APPROVED".equals(merchant.getStatus())) {
            throw new BusinessException("仅审核通过的商户可分配摊位");
        }
        Stall stall = stallMapper.selectById(stallId);
        if (stall == null) {
            throw new BusinessException("摊位不存在");
        }
        if ("OCCUPIED".equals(stall.getStatus()) && !stallId.equals(merchant.getAssignedStallId())) {
            throw new BusinessException("目标摊位已被占用，请重新选择");
        }
        // 释放该商户原摊位
        if (merchant.getAssignedStallId() != null) {
            Stall old = stallMapper.selectById(merchant.getAssignedStallId());
            if (old != null) {
                old.setStatus("AVAILABLE");
                old.setOccupantMerchantId(null);
                stallMapper.updateById(old);
            }
        }
        // 占用新摊位
        stall.setStatus("OCCUPIED");
        stall.setOccupantMerchantId(merchantId);
        stallMapper.updateById(stall);

        merchant.setAssignedStallId(stallId);
        merchantMapper.updateById(merchant);

        // 操作留痕
        StallAssignLog log = new StallAssignLog();
        log.setOperator(operator);
        log.setNote("分配摊位 " + stall.getStallNo());
        log.setCreatedAt(LocalDateTime.now());
        assignLogMapper.insert(log);
    }

    /** 摊位平面图数据：摊位 + 占用商户名 + 当前商户是否竞标 */
    public List<Map<String, Object>> mapData(Long merchantId) {
        List<Stall> stalls = listAll();
        Map<Long, String> merchantNames = new HashMap<>();
        List<Merchant> merchants = merchantMapper.selectList(null);
        for (Merchant m : merchants) {
            merchantNames.put(m.getId(), m.getName());
        }
        List<Long> myBids = merchantId == null ? List.of() : myBids(merchantId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Stall s : stalls) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", s.getId());
            item.put("stallNo", s.getStallNo());
            item.put("name", s.getName());
            item.put("level", s.getLevel());
            item.put("area", s.getArea());
            item.put("gridX", s.getGridX());
            item.put("gridY", s.getGridY());
            item.put("status", s.getStatus());
            item.put("occupant", s.getOccupantMerchantId() == null ? null
                    : merchantNames.get(s.getOccupantMerchantId()));
            item.put("myBid", myBids.contains(s.getId()));
            result.add(item);
        }
        return result;
    }
}
