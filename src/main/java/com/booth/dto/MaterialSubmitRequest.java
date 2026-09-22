package com.booth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 材料提交请求
 */
@Data
public class MaterialSubmitRequest {

    /** 模块：SOURCE/CULTURE/STANDARD/STALL */
    @NotBlank(message = "模块不能为空")
    private String module;

    @NotBlank(message = "请填写文字描述")
    private String content;
}
