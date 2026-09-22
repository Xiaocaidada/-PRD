package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动态导航栏目（最多两级）
 */
@Data
@TableName("nav_item")
public class NavItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父级ID，0 表示一级栏目 */
    private Long parentId;

    /** 栏目名称（一级限 10 字符） */
    private String name;

    /** 排序 */
    private Integer sort;

    /** 跳转地址 */
    private String url;

    private LocalDateTime createdAt;
}
