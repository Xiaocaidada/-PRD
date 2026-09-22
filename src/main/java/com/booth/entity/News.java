package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新闻/活动
 * 状态: PUBLISHED 已发布 / OFFLINE 已下线
 */
@Data
@TableName("news")
public class News {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private Long categoryId;

    private String summary;

    private String content;

    private String coverUrl;

    private String linkUrl;


    /** 是否重要通知(0/1) */
    private Integer isImportant;

    private String status;

    private String activityTime;

    private String activityLocation;

    private String registerDeadline;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
