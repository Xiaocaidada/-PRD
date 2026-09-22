package com.booth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.booth.dto.AssignedMerchantDTO;
import com.booth.dto.CandidateMerchantDTO;
import com.booth.entity.MarketBid;
import com.booth.entity.Material;
import com.booth.entity.Merchant;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface MerchantMapper extends BaseMapper<Merchant> {

    /**
     * 查询【待分配商户列表】，传入marketId
     */
    @Select("""
            SELECT m.id, m.name, m.license_no, m.categories, m.status, b.stall_no,b.remark, #{marketId} as marketId , 'PENDING' as mat_status
            FROM merchant m
            LEFT JOIN market_bid b ON m.id = b.merchant_id AND b.market_id = #{marketId}
            LEFT JOIN material mat ON m.id = mat.merchant_id AND mat.market_id = #{marketId}
            WHERE b.status = 'PENDING' 
            """)
    @Results({
            @Result(property = "id", column = "id"), // 加上 id=true！！！
            @Result(property = "name", column = "name"),
            @Result(property = "licenseNo", column = "license_no"),
            @Result(property = "categories", column = "categories"),
            @Result(property = "stallNo", column = "stall_no"),
            @Result(property = "remark", column = "remark"),
            @Result(property = "bidList",
                    column = "{merchantId=id, marketId=marketId}",
                    many = @Many(select = "getBidListByMerchantAndMarket")),
            @Result(property = "materialList",
                    column = "{merchantId=id, marketId=marketId,status=mat_status}",
                    many = @Many(select = "getMaterialListByMerchantAndMarket"))
    })
    List<CandidateMerchantDTO> selectCandidateMerchantList(@Param("marketId") Long marketId);

    /**
     * 查询【已分配商户列表】，传入marketId
     */
    @Select("""
            SELECT m.id, m.name, m.license_no, m.categories, bd.stall_no,  #{marketId} as marketId, 'APPROVED' as mat_status
            FROM merchant m
            LEFT JOIN market_bid bd ON m.id= bd.merchant_id AND bd.market_id = #{marketId}
            LEFT JOIN material mt ON m.id=mt.merchant_id AND mt.market_id= #{marketId}
            WHERE bd.status = 'APPROVED'
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "licenseNo", column = "license_no"),
            @Result(property = "categories", column = "categories"),
            @Result(property = "stallNo", column = "stall_no"),
            @Result(property = "materialList",
                    column = "{merchantId=id, marketId=marketId,status=mat_status}",
                    many = @Many(select = "getMaterialListByMerchantAndMarket"))
    })
    List<AssignedMerchantDTO> selectAssignedMerchantList(@Param("marketId") Long marketId);

    /**
     * 子查询：根据商户id + marketId 查询竞标记录
     */
    @Select("""
            SELECT id, stall_no
            FROM market_bid
            WHERE merchant_id = #{merchantId} AND market_id = #{marketId}
            """)
    List<MarketBid> getBidListByMerchantAndMarket(@Param("merchantId") Long merchantId, @Param("marketId") Long marketId);

    /**
     * 子查询：根据商户id + marketId 查询申报材料
     */
    @Select("""
        SELECT id, merchant_id, market_id, `module`, content, file_path, status, reject_reason, created_at, reviewed_at
        FROM material
        WHERE merchant_id = #{merchantId} AND market_id = #{marketId} AND status = #{status}
""")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "merchantId", column = "merchant_id"),
            @Result(property = "marketId", column = "market_id"),
            @Result(property = "module", column = "module"),
            @Result(property = "content", column = "content"),
            // JSON字段必须指定JacksonTypeHandler
            @Result(property = "filePath", column = "file_path", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class),
            @Result(property = "status", column = "status"),
            @Result(property = "rejectReason", column = "reject_reason"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "reviewedAt", column = "reviewed_at")
    })
    List<Material> getMaterialListByMerchantAndMarket(@Param("merchantId") Long merchantId, @Param("marketId") Long marketId,@Param("status") String status);
}
