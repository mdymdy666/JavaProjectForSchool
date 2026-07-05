package com.campustrade.message;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface MessageMapper extends BaseMapper<SiteMessage> {
    @Select("""
            SELECT COUNT(*) > 0 FROM orders
            WHERE product_id = #{productId} AND deleted = 0
              AND ((buyer_id = #{firstUserId} AND seller_id = #{secondUserId})
                OR (buyer_id = #{secondUserId} AND seller_id = #{firstUserId}))
            """)
    boolean areTradeParticipants(@Param("productId") long productId,
            @Param("firstUserId") long firstUserId,
            @Param("secondUserId") long secondUserId);
}
