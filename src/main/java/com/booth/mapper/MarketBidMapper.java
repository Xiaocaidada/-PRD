package com.booth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.booth.dto.CandidateMerchantDTO;
import com.booth.entity.MarketBid;

public interface MarketBidMapper extends BaseMapper<MarketBid> {

    public CandidateMerchantDTO selectCandidateMerchantById(Long id);
}
