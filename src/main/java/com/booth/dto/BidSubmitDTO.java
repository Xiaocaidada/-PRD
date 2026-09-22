package com.booth.dto;

import lombok.Data;

import java.util.List;

@Data
public class BidSubmitDTO {
    /**
     * 选中摊位编号数组
     */
    private List<String> stallNos;
    /**
     * 竞标备注
     */
    private String remark;
    /**
     * 材料条目列表
     */
    private List<BidMaterialDTO> materials;
}
