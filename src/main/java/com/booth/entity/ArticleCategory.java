package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章分类
 */
@Data
@TableName("article_category")
public class ArticleCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    private String name;

    /** 是否新闻中心固定展示(0/1) */
    private Integer fixedOnHome;

    /** 自动生成分类页 URL */
    private String url;

    private LocalDateTime createdAt;
}
