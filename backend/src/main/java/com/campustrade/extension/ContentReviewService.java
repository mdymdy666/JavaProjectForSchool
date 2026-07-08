package com.campustrade.extension;

import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentReviewService {
    private static final Set<String> DEFAULT_WORDS = Set.of(
            "违禁品", "假钞", "枪支", "毒品", "诈骗"
    );

    private final JdbcTemplate jdbc;

    public ContentReviewService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean containsSensitive(String text) {
        if (text == null) return false;
        String normalized = text.trim();
        for (String word : activeWords()) {
            if (!word.isBlank() && normalized.contains(word)) return true;
        }
        return false;
    }

    public void assertClean(String text) {
        if (containsSensitive(text)) {
            throw new com.campustrade.common.BusinessException(
                    com.campustrade.common.ErrorCode.BAD_REQUEST, "内容包含敏感词，请修改后重试");
        }
    }

    public List<SensitiveWordView> listWords() {
        return jdbc.query("""
                SELECT id, word, enabled FROM sensitive_words ORDER BY id DESC
                """, (rs, i) -> new SensitiveWordView(
                rs.getLong("id"), rs.getString("word"), rs.getInt("enabled") == 1));
    }

    @Transactional
    public SensitiveWordView addWord(String word) {
        String normalized = normalizeWord(word);
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sensitive_words WHERE word = ?",
                Integer.class, normalized);
        if (existing != null && existing > 0) {
            jdbc.update("UPDATE sensitive_words SET enabled = 1 WHERE word = ?", normalized);
        } else {
            jdbc.update("INSERT INTO sensitive_words(word, enabled) VALUES (?, 1)", normalized);
        }
        return jdbc.queryForObject("""
                SELECT id, word, enabled FROM sensitive_words WHERE word = ?
                """, (rs, i) -> new SensitiveWordView(
                rs.getLong("id"), rs.getString("word"), rs.getInt("enabled") == 1), normalized);
    }

    @Transactional
    public void setEnabled(long id, boolean enabled) {
        jdbc.update("UPDATE sensitive_words SET enabled = ? WHERE id = ?", enabled ? 1 : 0, id);
    }

    private List<String> activeWords() {
        List<String> dbWords = jdbc.query("""
                SELECT word FROM sensitive_words WHERE enabled = 1
                """, (rs, i) -> rs.getString("word"));
        return dbWords.isEmpty() ? DEFAULT_WORDS.stream().toList() : dbWords;
    }

    private String normalizeWord(String word) {
        if (word == null || word.trim().isBlank()) {
            throw new com.campustrade.common.BusinessException(
                    com.campustrade.common.ErrorCode.BAD_REQUEST, "敏感词不能为空");
        }
        return word.trim();
    }

    public record SensitiveWordView(long id, String word, boolean enabled) {}
}
