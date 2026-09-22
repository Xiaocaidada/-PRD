package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 市集
 * 状态: PREPARING筹备中/PUBLISHED已发布/ONGOING进行中/FINISHED已结束/OFFLINE已下线
 */
@Data
@TableName("market")
public class Market {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 市集名称 */
    private String name;

    /** 举办地点 */
    private String location;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 竞标报名截止时间 */
    private LocalDateTime bidDeadline;

    /** 活动声明 */
    private String statement;

    /** 关联布局模板ID */
    private Long templateId;

    /** 绑定模板时的布局快照(JSON) */
    private String layoutSnapshot;

    /** 竞标开关 0关/1开 */
    private Integer bidEnabled;

    /** 状态 */
    private String status;

    private String imageUrl;

    /** 下线前状态(重新发布恢复用) */
    private String offlineBeforeStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
