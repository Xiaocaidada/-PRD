package com.booth.dto;

import lombok.Data;

/**
 * 摊位分配请求
 */
@Data
public class AssignStallRequest {

    private Long merchantId;

    private Long stallId;
}
