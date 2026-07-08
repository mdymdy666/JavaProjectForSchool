package com.campustrade.extension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RecommendationService {
    private static final int CANDIDATE_LIMIT = 120;

    private final JdbcTemplate jdbc;

    public RecommendationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record RecommendProduct(long id, String title, String categoryName,
            BigDecimal price, String imageUrl, int viewCount, double score, String reason) {}

    private record Candidate(long id, long sellerId, long categoryId, String title,
            String categoryName, BigDecimal price, String itemCondition, String imageUrl,
            int viewCount, LocalDateTime createdAt) {}

    private record Behavior(Long productId, String behaviorType, String keyword,
            Long categoryId, String categoryName, String title, String description,
            BigDecimal price, String itemCondition, int weight, LocalDateTime createdAt) {}

    public void recordSearch(long userId, String keyword, Long categoryId) {
        if (!StringUtils.hasText(keyword) && categoryId == null) return;
        recordBehavior(userId, null, "SEARCH", keyword, categoryId, 14);
    }

    public void recordProductBehavior(long userId, long productId, String behaviorType) {
        int weight = switch (behaviorType) {
            case "FAVORITE" -> 30;
            case "PURCHASE" -> 45;
            case "VIEW" -> 8;
            default -> 5;
        };
        Long categoryId = jdbc.queryForObject(
                "SELECT category_id FROM products WHERE id = ?",
                Long.class, productId);
        recordBehavior(userId, productId, behaviorType, null, categoryId, weight);
    }

    public List<RecommendProduct> recommend(Long userId, int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, 20));
        List<Candidate> candidates = loadCandidates(userId);
        if (candidates.isEmpty()) return List.of();

        if (userId == null) {
            return candidates.stream()
                    .map(candidate -> toRecommend(candidate, coldStartScore(candidate), "校园热门商品"))
                    .sorted(Comparator.comparingDouble(RecommendProduct::score).reversed())
                    .limit(limit)
                    .toList();
        }

        List<Behavior> behaviors = loadBehaviors(userId);
        if (behaviors.isEmpty()) {
            return candidates.stream()
                    .map(candidate -> toRecommend(candidate, coldStartScore(candidate), "先按热度和新鲜度推荐"))
                    .sorted(Comparator.comparingDouble(RecommendProduct::score).reversed())
                    .limit(limit)
                    .toList();
        }

        UserProfile profile = buildProfile(behaviors);
        return candidates.stream()
                .map(candidate -> scoreCandidate(candidate, profile))
                .sorted(Comparator.comparingDouble(RecommendProduct::score).reversed())
                .limit(limit)
                .toList();
    }

    private void recordBehavior(long userId, Long productId, String behaviorType,
            String keyword, Long categoryId, int weight) {
        jdbc.update("""
                INSERT INTO user_behaviors(user_id, product_id, behavior_type, keyword, category_id, weight)
                VALUES (?, ?, ?, ?, ?, ?)
                """, userId, productId, behaviorType,
                StringUtils.hasText(keyword) ? keyword.trim() : null,
                categoryId, weight);
    }

    private List<Candidate> loadCandidates(Long userId) {
        return jdbc.query("""
                SELECT p.id, p.seller_id, p.category_id, p.title, c.name AS category_name,
                       p.price, p.item_condition,
                       (SELECT pi.image_url FROM product_images pi
                        WHERE pi.product_id = p.id ORDER BY pi.sort_order LIMIT 1) AS image_url,
                       p.view_count, p.created_at
                FROM products p
                JOIN categories c ON c.id = p.category_id
                WHERE p.status = 'APPROVED' AND p.deleted = 0
                  AND (? IS NULL OR p.seller_id <> ?)
                ORDER BY p.view_count DESC, p.created_at DESC
                LIMIT ?
                """, (rs, i) -> new Candidate(
                rs.getLong("id"),
                rs.getLong("seller_id"),
                rs.getLong("category_id"),
                rs.getString("title"),
                rs.getString("category_name"),
                rs.getBigDecimal("price"),
                rs.getString("item_condition"),
                rs.getString("image_url"),
                rs.getInt("view_count"),
                toLocalDateTime(rs.getTimestamp("created_at"))),
                userId, userId, CANDIDATE_LIMIT);
    }

    private List<Behavior> loadBehaviors(long userId) {
        return jdbc.query("""
                SELECT ub.product_id, ub.behavior_type, ub.keyword,
                       COALESCE(ub.category_id, p.category_id) AS category_id,
                       c.name AS category_name, p.title, p.description, p.price,
                       p.item_condition, ub.weight, ub.created_at
                FROM user_behaviors ub
                LEFT JOIN products p ON p.id = ub.product_id
                LEFT JOIN categories c ON c.id = COALESCE(ub.category_id, p.category_id)
                WHERE ub.user_id = ?
                ORDER BY ub.created_at DESC
                LIMIT 200
                """, (rs, i) -> {
                    long productId = rs.getLong("product_id");
                    Long boxedProductId = rs.wasNull() ? null : productId;
                    long categoryId = rs.getLong("category_id");
                    Long boxedCategoryId = rs.wasNull() ? null : categoryId;
                    return new Behavior(
                            boxedProductId,
                            rs.getString("behavior_type"),
                            rs.getString("keyword"),
                            boxedCategoryId,
                            rs.getString("category_name"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getBigDecimal("price"),
                            rs.getString("item_condition"),
                            rs.getInt("weight"),
                            toLocalDateTime(rs.getTimestamp("created_at")));
                }, userId);
    }

    private UserProfile buildProfile(List<Behavior> behaviors) {
        UserProfile profile = new UserProfile();
        for (Behavior behavior : behaviors) {
            double weighted = behavior.weight() * timeDecay(behavior.createdAt());
            if (behavior.categoryId() != null) {
                profile.categoryScores.merge(behavior.categoryId(), weighted, Double::sum);
                if (StringUtils.hasText(behavior.categoryName())) {
                    profile.categoryNames.putIfAbsent(behavior.categoryId(), behavior.categoryName());
                }
            }
            if (StringUtils.hasText(behavior.keyword())) {
                profile.keywordScores.merge(normalize(behavior.keyword()), weighted, Double::sum);
            }
            if (StringUtils.hasText(behavior.itemCondition())) {
                profile.conditionScores.merge(behavior.itemCondition(), weighted * 0.7, Double::sum);
            }
            if (behavior.price() != null) {
                profile.weightedPrice += behavior.price().doubleValue() * weighted;
                profile.priceWeight += weighted;
            }
            if (behavior.productId() != null) {
                profile.touchedProducts.add(behavior.productId());
                if ("FAVORITE".equals(behavior.behaviorType()) || "PURCHASE".equals(behavior.behaviorType())) {
                    profile.strongTouchedProducts.add(behavior.productId());
                }
            }
        }
        return profile;
    }

    private RecommendProduct scoreCandidate(Candidate candidate, UserProfile profile) {
        double score = coldStartScore(candidate);
        String reason = "根据热度和新鲜度推荐";

        double categoryScore = profile.categoryScores.getOrDefault(candidate.categoryId(), 0.0);
        if (categoryScore > 0) {
            score += categoryScore * 3.5;
            reason = "根据你最近关注的" + candidate.categoryName() + "推荐";
        }

        KeywordMatch keywordMatch = keywordMatch(candidate, profile.keywordScores);
        if (keywordMatch.score > 0) {
            score += keywordMatch.score * 3.0;
            reason = "匹配你搜索过的“" + keywordMatch.keyword + "”";
        }

        double conditionScore = profile.conditionScores.getOrDefault(candidate.itemCondition(), 0.0);
        score += conditionScore * 1.2;

        if (profile.priceWeight > 0) {
            double avgPrice = profile.weightedPrice / profile.priceWeight;
            double diffRatio = Math.abs(candidate.price().doubleValue() - avgPrice) / Math.max(avgPrice, 1);
            score += Math.max(0, 14 - diffRatio * 14);
        }

        if (profile.strongTouchedProducts.contains(candidate.id())) {
            score -= 24;
        } else if (profile.touchedProducts.contains(candidate.id())) {
            score -= 6;
        }

        return toRecommend(candidate, score, reason);
    }

    private KeywordMatch keywordMatch(Candidate candidate, Map<String, Double> keywordScores) {
        String haystack = normalize(candidate.title() + " " + candidate.categoryName());
        KeywordMatch best = new KeywordMatch("", 0);
        for (Map.Entry<String, Double> entry : keywordScores.entrySet()) {
            String keyword = entry.getKey();
            if (keyword.length() >= 2 && haystack.contains(keyword) && entry.getValue() > best.score) {
                best = new KeywordMatch(keyword, entry.getValue());
            }
        }
        return best;
    }

    private RecommendProduct toRecommend(Candidate candidate, double score, String reason) {
        return new RecommendProduct(candidate.id(), candidate.title(), candidate.categoryName(),
                candidate.price(), candidate.imageUrl(), candidate.viewCount(),
                Math.round(score * 100.0) / 100.0, reason);
    }

    private double coldStartScore(Candidate candidate) {
        return Math.log(candidate.viewCount() + 1) * 4 + recencyScore(candidate.createdAt());
    }

    private double recencyScore(LocalDateTime createdAt) {
        long days = Math.max(0, Duration.between(createdAt, LocalDateTime.now()).toDays());
        return Math.max(0, 12 - days * 0.8);
    }

    private double timeDecay(LocalDateTime createdAt) {
        long days = Math.max(0, Duration.between(createdAt, LocalDateTime.now()).toDays());
        return 1.0 / (1.0 + days / 14.0);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? LocalDateTime.now() : timestamp.toLocalDateTime();
    }

    private static final class UserProfile {
        private final Map<Long, Double> categoryScores = new HashMap<>();
        private final Map<Long, String> categoryNames = new HashMap<>();
        private final Map<String, Double> keywordScores = new HashMap<>();
        private final Map<String, Double> conditionScores = new HashMap<>();
        private final Set<Long> touchedProducts = new HashSet<>();
        private final Set<Long> strongTouchedProducts = new HashSet<>();
        private double weightedPrice;
        private double priceWeight;
    }

    private static final class KeywordMatch {
        private final String keyword;
        private final double score;

        private KeywordMatch(String keyword, double score) {
            this.keyword = keyword;
            this.score = score;
        }
    }
}
