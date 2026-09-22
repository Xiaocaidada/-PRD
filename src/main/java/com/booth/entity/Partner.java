package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文化合作单位
 * 分类: 学术指导单位 / 学校科普单位 / 产学研单位
 */
@Data
@TableName("partner")
public class Partner {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String category;

    private Integer sort;

    private String linkUrl;

    private String status;

    private LocalDateTime createdAt;
}
