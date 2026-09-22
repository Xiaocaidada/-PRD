package com.booth.dto;

import lombok.Data;

/**
 * 审核请求
 */
@Data
public class ReviewRequest {

    private Long materialId;

    /** true 通过 / false 驳回 */
    private Boolean approve;

    /** 驳回原因（驳回时必填） */
    private String rejectReason;
}
