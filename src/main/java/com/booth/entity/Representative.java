package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创意创新代表人物
 */
@Data
@TableName("representative")
public class Representative {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String title;

    /** 头像（1:1） */
    private String avatar;

    private Integer sort;

    private String status;

    private LocalDateTime createdAt;
}
