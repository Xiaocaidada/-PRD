package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 摊位分配记录（操作留痕）
 */
@Data
@TableName("stall_assign_log")
public class StallAssignLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String operator;

    private String note;

    private LocalDateTime createdAt;
}
