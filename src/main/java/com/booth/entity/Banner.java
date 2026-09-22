package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Banner（3:1 轮播图）
 * 状态: ON 上架 / HIDDEN 隐藏
 */
@Data
@TableName("banner")
public class Banner {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /** 图片地址 */
    private String imageUrl;

    /** 点击跳转链接 */
    private String linkUrl;

    private Integer sort;

    private String status;

    private LocalDateTime createdAt;
}
