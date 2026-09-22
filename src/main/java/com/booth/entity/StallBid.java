package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 竞标摊位（商户选择）
 */
@Data
@TableName("stall_bid")
public class StallBid {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    private Long stallId;

    private LocalDateTime createdAt;
}
