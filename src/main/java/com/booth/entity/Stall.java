package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 摊位
 * 状态: AVAILABLE 可用 / OCCUPIED 已占用
 */
@Data
@TableName("stall")
public class Stall {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 摊位编号，如 A1 */
    private String stallNo;

    /** 摊位名称 */
    private String name;

    /** 摊位级别（同级别可多选竞标） */
    private Integer level;

    /** 区域 */
    private String area;

    /** 平面图横坐标 */
    private Integer gridX;

    /** 平面图纵坐标 */
    private Integer gridY;

    /** 状态 */
    private String status;

    /** 占用商户ID */
    private Long occupantMerchantId;

    private LocalDateTime createdAt;
}
