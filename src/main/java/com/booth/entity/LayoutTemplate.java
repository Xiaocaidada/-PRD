package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 市集布局模板
 * 状态: ENABLED 启用 / DISABLED 禁用
 */
@Data
@TableName("layout_template")
public class LayoutTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板名称 */
    private String name;

    /** 画布宽度(mm) */
    private Integer canvasWidth;

    /** 画布高度(mm) */
    private Integer canvasHeight;

    /** 网格间距(mm) */
    private Integer gridSize;

    /** 底图(base64) */
    private String background;

    /** 元素JSON数组 */
    private String elements;

    /** 状态 */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 摊位元素数量（非表字段，列表展示用） */
    @TableField(exist = false)
    private Integer stallCount;

    /** 绑定该模板的市集数量（非表字段，列表展示用） */
    @TableField(exist = false)
    private Integer marketCount;
}
