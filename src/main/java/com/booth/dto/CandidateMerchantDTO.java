package com.booth.dto;
import com.booth.entity.MarketBid;
import com.booth.entity.Material;
import lombok.Data;
import java.util.List;

@Data
public class CandidateMerchantDTO {
    // 商户基础信息
    private Long id;
    private String name;
    private String licenseNo;
    private String categories;
    private String stallNo;
    private String remark;

    // 当前市集下该商户的竞标记录
    private List<MarketBid> bidList;
    // 商户提交的材料列表
    private List<Material> materialList;
}
