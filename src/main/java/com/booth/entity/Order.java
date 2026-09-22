package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 市集订单（统计用）
 */
@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long merchantId;

    private Long stallId;

    private Long activityId;

    private BigDecimal amount;

    private BigDecimal refundAmount;

    /** PAID 已支付 / REFUNDED 已退款 */
    private String status;

    private LocalDateTime createdAt;
}
