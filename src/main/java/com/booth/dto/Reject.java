package com.booth.dto;

import lombok.Data;

@Data
public class Reject {
    private Long marketId;
    private String stallNo;
    private String reason;
    private Long merchantId;

}
