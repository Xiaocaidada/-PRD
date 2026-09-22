package com.booth.dto;

import lombok.Data;

import java.util.List;

@Data
public class BidMaterialDTO {
    /**
     * 模块标识：SOURCE / CULTURE / STANDARD / STALL
     */
    private String module;
    /**
     * 文字描述内容
     */
    private String content;
    /**
     * 文件url数组，就是前端上传文件返回的地址
     */
    private List<String> urls;
}
