package com.booth.dto;
import com.booth.entity.Material;
import lombok.Data;

import java.util.List;

@Data
public class AssignedMerchantDTO {
    private Long id;
    private String name;
    private String licenseNo;
    private String categories;
    private String stallNo;
    private List<Material> materialList;
}
