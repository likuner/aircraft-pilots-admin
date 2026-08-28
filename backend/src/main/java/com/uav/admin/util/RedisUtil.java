package com.uav.admin.util;

import com.uav.admin.common.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Redis 工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final StringRedisTemplate stringRedisTemplate;

    public void set(String key, String value, Duration timeout) {
        stringRedisTemplate.opsForValue().set(key, value, timeout);
    }

    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    public long increment(String key) {
        return stringRedisTemplate.opsForValue().increment(key, 1);
    }

    public boolean setIfAbsent(String key, String value, Duration timeout) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, timeout));
    }

    public void expire(String key, long timeout, TimeUnit unit) {
        stringRedisTemplate.expire(key, timeout, unit);
    }

    /** 分布式锁（简单版，含过期防止死锁） */
    public boolean tryLock(String bizKey, Duration timeout) {
        return setIfAbsent(Constants.LOCK_PREFIX + bizKey, "1", timeout);
    }

    public void unlock(String bizKey) {
        delete(Constants.LOCK_PREFIX + bizKey);
    }

    /**
     * 业务编号生成：Redis 自增（如 UVA-2026-000001）。
     * 若生成号已存在（Redis 计数器因重启/清空被重置），则基于 DB 现有最大号+1 重算并
     * 同步重置计数器，保证编号唯一，不依赖 Redis 持久化。
     *
     * @param redisKey      自增计数器 key
     * @param prefix        编号前缀（含年份），如 "UVA-2026-"
     * @param existsChecker 判断该编号是否已存在（DB 查询）
     * @param maxNoSupplier 返回当前已存在的最大完整编号（无则返回 null）
     */
    public String nextBizNo(String redisKey, String prefix, Predicate<String> existsChecker, Supplier<String> maxNoSupplier) {
        long seq = increment(redisKey);
        String no = prefix + String.format("%06d", seq);
        if (existsChecker.test(no)) {
            log.warn("业务编号 {} 已存在（Redis 计数器被重置？），基于 DB 最大号重新生成", no);
            String last = maxNoSupplier.get();
            long base = 1;
            if (last != null && !last.isEmpty()) {
                base = Long.parseLong(last.substring(last.lastIndexOf('-') + 1)) + 1;
            }
            stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(base));
            no = prefix + String.format("%06d", base);
        }
        return no;
    }
}
