package com.campustrade.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RedisSupport {
    private final boolean enabled;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisSupport(@Value("${campus-trade.redis-enabled:true}") boolean enabled,
            ObjectProvider<StringRedisTemplate> provider, ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.redis = provider.getIfAvailable();
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> getJson(String key, Class<T> type) {
        if (!available()) return Optional.empty();
        try {
            String value = redis.opsForValue().get(key);
            return value == null ? Optional.empty() : Optional.of(objectMapper.readValue(value, type));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public void putJson(String key, Object value, Duration ttl) {
        if (!available()) return;
        try { redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl); }
        catch (Exception ignored) { }
    }

    public Optional<String> get(String key) {
        if (!available()) return Optional.empty();
        try { return Optional.ofNullable(redis.opsForValue().get(key)); }
        catch (RuntimeException ignored) { return Optional.empty(); }
    }

    public void put(String key, String value, Duration ttl) {
        if (!available()) return;
        try { redis.opsForValue().set(key, value, ttl); } catch (RuntimeException ignored) { }
    }

    public OptionalLong incrementBy(String key, long delta, Duration ttl) {
        if (!available()) return OptionalLong.empty();
        try {
            Long value = redis.opsForValue().increment(key, delta);
            if (value != null && value.equals(delta)) redis.expire(key, ttl);
            return value == null ? OptionalLong.empty() : OptionalLong.of(value);
        } catch (RuntimeException ignored) { return OptionalLong.empty(); }
    }

    public boolean exists(String key) {
        if (!available()) return false;
        try { return Boolean.TRUE.equals(redis.hasKey(key)); }
        catch (RuntimeException ignored) { return false; }
    }

    public OptionalLong increment(String key, Duration ttl) {
        if (!available()) return OptionalLong.empty();
        try {
            Long value = redis.opsForValue().increment(key);
            if (value != null && value == 1) redis.expire(key, ttl);
            return value == null ? OptionalLong.empty() : OptionalLong.of(value);
        } catch (RuntimeException ignored) { return OptionalLong.empty(); }
    }

    public void incrementScore(String key, String member) {
        if (!available()) return;
        try { redis.opsForZSet().incrementScore(key, member, 1); } catch (RuntimeException ignored) { }
    }

    public void delete(String key) {
        if (!available()) return;
        try { redis.delete(key); } catch (RuntimeException ignored) { }
    }

    public boolean isAvailable() { return available(); }

    private boolean available() { return enabled && redis != null; }
}
