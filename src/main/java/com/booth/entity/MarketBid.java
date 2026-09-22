package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 市集竞标记录
 * 状态: PENDING待审核/APPROVED已中标/REJECTED未中标
 */
@Data
@TableName("market_bid")
public class MarketBid {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 市集ID */
    private Long marketId;

    /** 商户ID */
    private Long merchantId;

    /** 竞标摊位编号(对应模板摊位) */
    private String stallNo;

    /** 竞标备注 */
    private String remark;

    /** 状态 */
    private String status;

    private LocalDateTime createdAt;
}
