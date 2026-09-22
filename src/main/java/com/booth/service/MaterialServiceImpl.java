package com.booth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.booth.common.BusinessException;
import com.booth.entity.Material;
import com.booth.mapper.MaterialMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 补充材料服务
 */
@Service
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper,Material> implements MaterialService  {

    /** 每模块最多条目数 */
    public static final int MAX_PER_MODULE = 5;

    private final MaterialMapper materialMapper;

    public MaterialServiceImpl(MaterialMapper materialMapper) {
        this.materialMapper = materialMapper;
    }

    /** 商户提交材料（文本 + 可选附件） */

    @Override
    public List<Material> listByMerchant(Long merchantId,Long marketId) {
        return materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .eq(Material::getMerchantId,merchantId)
                .eq(Material::getMarketId,marketId));
    }


    @Override
    public void saveList(List<Material> items){
        for (Material item : items) {
            materialMapper.delete(new LambdaQueryWrapper<Material>()
                    .eq(Material::getMerchantId,item.getMerchantId())
                    .eq(Material::getMarketId,item.getMarketId())
                    .eq(Material::getStatus,"PENDING"));
        }
        if(items.isEmpty()) return ;
        saveBatch(items);
    }

    /** 管理员审核：通过 / 驳回 */
    public void review(Long materialId, boolean approve, String rejectReason) {
        Material material = materialMapper.selectById(materialId);
        UpdateWrapper<Material> wrapper=new UpdateWrapper<Material>().eq("id", materialId);
        if (material == null) {
            throw new BusinessException("材料不存在");
        }
        if (approve) {
            wrapper.set("status","APPROVED");
            wrapper.set("reject_reason",null);
        } else {
            if (rejectReason == null || rejectReason.trim().isEmpty()) {
                throw new BusinessException("驳回原因不能为空");
            }
            wrapper.set("status","REJECTED");
            wrapper.set("reject_reason",rejectReason.trim());
        }
        wrapper.set("reviewed_at",LocalDateTime.now());
        materialMapper.update(wrapper);
    }

    /** 某一模块已通过的内容（商户摊位信息卡展示用） */
    public List<Material> approvedByModule(Long merchantId, String module) {
        return materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .eq(Material::getMerchantId, merchantId)
                .eq(Material::getModule, module)
                .eq(Material::getStatus, "APPROVED")
                .orderByAsc(Material::getCreatedAt));
    }
}
