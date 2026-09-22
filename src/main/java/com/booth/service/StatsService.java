package com.booth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.booth.entity.Order;
import com.booth.entity.TrafficStat;
import com.booth.mapper.OrderMapper;
import com.booth.mapper.TrafficStatMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务：客流 / 订单 / 收支
 */
@Service
public class StatsService {

    private final OrderMapper orderMapper;
    private final TrafficStatMapper trafficStatMapper;

    public StatsService(OrderMapper orderMapper, TrafficStatMapper trafficStatMapper) {
        this.orderMapper = orderMapper;
        this.trafficStatMapper = trafficStatMapper;
    }

    /** 商户端收支统计 + 客流统计 */
    public Map<String, Object> merchantStats(Long merchantId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().eq(Order::getMerchantId, merchantId));
        BigDecimal totalAmount = orders.stream().map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundAmount = orders.stream().map(Order::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<TrafficStat> traffic = trafficStatMapper.selectList(
                new LambdaQueryWrapper<TrafficStat>().eq(TrafficStat::getMerchantId, merchantId));

        int totalVisitors = traffic.stream().mapToInt(TrafficStat::getVisitorCount).sum();

        Map<String, Object> result = new HashMap<>();
        result.put("orderCount", orders.size());
        result.put("totalAmount", totalAmount);
        result.put("refundAmount", refundAmount);
        result.put("netAmount", totalAmount.subtract(refundAmount));
        result.put("totalVisitors", totalVisitors);

        // 近 7 天客流趋势
        Map<String, Integer> trend = new HashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            trend.put(today.minusDays(i).toString(), 0);
        }
        for (TrafficStat t : traffic) {
            String key = t.getStatDate().toString();
            trend.merge(key, t.getVisitorCount(), Integer::sum);
        }
        result.put("trafficTrend", trend);
        return result;
    }

    /** 管理端统计：订单/成交/退款汇总 */
    public Map<String, Object> adminStats() {
        List<Order> orders = orderMapper.selectList(null);
        BigDecimal totalAmount = orders.stream().map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundAmount = orders.stream().map(Order::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<TrafficStat> traffic = trafficStatMapper.selectList(null);
        int totalVisitors = traffic.stream().mapToInt(TrafficStat::getVisitorCount).sum();

        Map<String, Object> result = new HashMap<>();
        result.put("orderCount", orders.size());
        result.put("totalAmount", totalAmount);
        result.put("refundAmount", refundAmount);
        result.put("totalVisitors", totalVisitors);

        // 按活动维度客流
        Map<Long, Integer> activityTraffic = new HashMap<>();
        for (TrafficStat t : traffic) {
            activityTraffic.merge(t.getActivityId() == null ? 0L : t.getActivityId(),
                    t.getVisitorCount(), Integer::sum);
        }
        result.put("activityTraffic", activityTraffic);
        return result;
    }

    /** 生成 CSV（订单明细） */
    public String ordersCsv() {
        List<Order> orders = orderMapper.selectList(null);
        StringBuilder sb = new StringBuilder("\uFEFF订单号,商户ID,摊位ID,金额,退款,状态,时间\n");
        for (Order o : orders) {
            sb.append(o.getOrderNo()).append(",")
                    .append(o.getMerchantId()).append(",")
                    .append(o.getStallId() == null ? "" : o.getStallId()).append(",")
                    .append(o.getAmount()).append(",")
                    .append(o.getRefundAmount()).append(",")
                    .append(o.getStatus()).append(",")
                    .append(o.getCreatedAt()).append("\n");
        }
        return sb.toString();
    }
}
