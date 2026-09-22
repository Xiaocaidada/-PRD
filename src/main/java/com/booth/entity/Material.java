package com.booth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 补充材料
 * 模块: SOURCE 商品货源 / CULTURE 文创内容 / STANDARD 生产标准 / STALL 文创市集摊位
 * 状态: PENDING 待审核 / APPROVED 已通过 / REJECTED 已驳回
 */
@Data
@TableName(value="material",autoResultMap = true)
public class Material {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    private Long marketId;

    /** 模块 */
    private String module;

    /** 文本描述 */
    private String content;

    /** 附件路径 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    List<String> filePath;

    /** 状态 */
    private String status;

    /** 驳回原因 */
    private String rejectReason;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;
}
