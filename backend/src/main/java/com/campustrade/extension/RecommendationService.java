package com.campustrade.extension;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {
    private final JdbcTemplate jdbc;

    public RecommendationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record RecommendProduct(long id, String title, String categoryName,
            BigDecimal price, String imageUrl, int viewCount) {}

    public List<RecommendProduct> recommend(int limit) {
        return jdbc.query("""
                SELECT p.id, p.title, c.name AS category_name, p.price,
                       (SELECT pi.image_url FROM product_images pi
                        WHERE pi.product_id = p.id ORDER BY pi.sort_order LIMIT 1) AS image_url,
                       p.view_count
                FROM products p
                JOIN categories c ON c.id = p.category_id
                WHERE p.status = 'APPROVED' AND p.deleted = 0
                ORDER BY p.view_count DESC, p.created_at DESC
                LIMIT ?
                """, (rs, i) -> new RecommendProduct(
                rs.getLong("id"), rs.getString("title"),
                rs.getString("category_name"), rs.getBigDecimal("price"),
                rs.getString("image_url"), rs.getInt("view_count")),
                limit);
    }
}
