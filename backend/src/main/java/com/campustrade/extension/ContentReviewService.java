package com.campustrade.extension;

import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ContentReviewService {
    private static final Set<String> DEFAULT_WORDS = Set.of(
            "违禁品", "假钞", "枪支", "毒品", "诈骗"
    );

    private final Set<String> words;

    public ContentReviewService() {
        this.words = DEFAULT_WORDS;
    }

    public boolean containsSensitive(String text) {
        if (text == null) return false;
        for (String word : words) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    public void assertClean(String text) {
        if (containsSensitive(text)) {
            throw new com.campustrade.common.BusinessException(
                    com.campustrade.common.ErrorCode.BAD_REQUEST, "内容包含敏感词，请修改后重试");
        }
    }
}
