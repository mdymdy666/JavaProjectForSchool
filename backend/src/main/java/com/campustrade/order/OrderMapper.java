package com.campustrade.order;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface OrderMapper extends BaseMapper<TradeOrder> {
    @Update("""
            UPDATE orders SET status = #{target}, logistics_info = #{logisticsInfo},
                version = version + 1, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = #{source} AND version = #{version} AND deleted = 0
            """)
    int transition(@Param("id") long id, @Param("source") String source,
            @Param("target") String target, @Param("version") int version,
            @Param("logisticsInfo") String logisticsInfo);

    @Insert("""
            INSERT INTO order_logs(order_id, from_status, to_status, operator_id, remark)
            VALUES (#{orderId}, #{fromStatus}, #{toStatus}, #{operatorId}, #{remark})
            """)
    int insertLog(@Param("orderId") long orderId, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("operatorId") long operatorId,
            @Param("remark") String remark);
}
