package com.campustrade.admin;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminMapper {
    @Select("SELECT COUNT(*) FROM users") long userCount();
    @Select("SELECT COUNT(*) FROM products WHERE deleted = 0") long productCount();
    @Select("SELECT COUNT(*) FROM orders WHERE deleted = 0") long orderCount();
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status = 'COMPLETED' AND deleted = 0")
    BigDecimal turnover();
    @Select("""
            SELECT c.name AS categoryName, COUNT(p.id) AS productCount
            FROM categories c LEFT JOIN products p ON p.category_id = c.id AND p.deleted = 0
            GROUP BY c.id, c.name ORDER BY productCount DESC, c.id ASC
            """)
    List<AdminDtos.CategoryStat> categoryStats();
}
