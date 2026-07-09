package com.campustrade.product;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    @Select("SELECT COUNT(*) > 0 FROM categories WHERE id = #{categoryId} AND enabled = 1")
    boolean categoryExists(@Param("categoryId") long categoryId);

    @Select("SELECT name FROM categories WHERE id = #{categoryId}")
    String categoryName(@Param("categoryId") long categoryId);

    @Select("""
            SELECT id FROM categories
            WHERE enabled = 1 AND name LIKE CONCAT('%', #{keyword}, '%')
            """)
    List<Long> categoryIdsLike(@Param("keyword") String keyword);

    @Insert("""
            INSERT INTO audit_logs(admin_id, target_type, target_id, action, reason)
            VALUES (#{adminId}, 'PRODUCT', #{productId}, #{action}, #{reason})
            """)
    int insertAuditLog(
            @Param("adminId") long adminId,
            @Param("productId") long productId,
            @Param("action") String action,
            @Param("reason") String reason);

    @Insert("""
            INSERT INTO notifications(user_id, type, title, content, read_status)
            VALUES (#{userId}, #{type}, #{title}, #{content}, 'UNREAD')
            """)
    int insertNotification(
            @Param("userId") long userId,
            @Param("type") String type,
            @Param("title") String title,
            @Param("content") String content);

    @Update("""
            UPDATE products
            SET status = 'DELETED', deleted = 1, version = version + 1
            WHERE id = #{productId} AND seller_id = #{sellerId} AND deleted = 0
            """)
    int softDelete(@Param("productId") long productId, @Param("sellerId") long sellerId);

    @Update("""
            UPDATE products SET view_count = view_count + 1
            WHERE id = #{productId} AND deleted = 0
            """)
    int incrementViewCount(@Param("productId") long productId);

    @Update("""
            UPDATE products SET status = 'SOLD', version = version + 1
            WHERE id = #{productId} AND status = 'APPROVED' AND version = #{version} AND deleted = 0
            """)
    int markSold(@Param("productId") long productId, @Param("version") int version);

    @Update("""
            UPDATE products SET status = 'APPROVED', version = version + 1
            WHERE id = #{productId} AND status = 'SOLD' AND deleted = 0
            """)
    int restoreApproved(@Param("productId") long productId);
}
