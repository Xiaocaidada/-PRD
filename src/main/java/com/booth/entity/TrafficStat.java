package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客流统计
 */
@Data
@TableName("traffic_stat")
public class TrafficStat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private Long merchantId;

    private Long stallId;

    private LocalDate statDate;

    /** 时段 */
    private Integer hourSlot;

    private Integer visitorCount;

    private LocalDateTime createdAt;
}
