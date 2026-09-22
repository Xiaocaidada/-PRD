package com.booth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.booth.entity.Material;

import java.util.List;

public interface MaterialService extends IService<Material> {

    void saveList(List<Material> items);
    public List<Material> listByMerchant(Long merchantId,Long marketId);
    public void review(Long materialId, boolean approve, String rejectReason);
    public List<Material> approvedByModule(Long merchantId, String module);
}
