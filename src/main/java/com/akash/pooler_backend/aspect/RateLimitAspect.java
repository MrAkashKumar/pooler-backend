package com.akash.pooler_backend.aspect;

import com.akash.pooler_backend.exception.RateLimitException;
import com.akash.pooler_backend.interceptors.annotation.RateLimit;
import com.akash.pooler_backend.utils.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AOP Aspect — enforces {@link @RateLimit} on annotated controller methods.
 *
 * Implementation: in-memory sliding window counter per (IP + endpoint) key.
 * For production at scale, replace with Redis (e.g. Redisson) to share
 * state across multiple pod replicas.
 *
 * Designed as a Strategy Pattern — the rate-limiting algorithm can be swapped
 * by changing the implementation behind the RateLimiter interface without
 * touching controllers.
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    /** key → (windowStartEpochMs, requestCount) */
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Before("@annotation(rateLimit)")
    public void enforce(JoinPoint jp, RateLimit rateLimit) {
        HttpServletRequest req;
        try {
            req = RequestUtil.currentRequest();
        } catch (Exception e) {
            return; // can't enforce without request context — fail open
        }

        String ip       = RequestUtil.getClientIp(req);
        String endpoint = buildEndpointKey(jp);
        String key      = rateLimit.key().isBlank() ? ip + ":" + endpoint : rateLimit.key() + ":" + ip;

        long windowMs = rateLimit.windowSeconds() * 1000L;
        long now      = Instant.now().toEpochMilli();

        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || (now - existing.windowStart) > windowMs) {
                return new WindowCounter(now); // new window
            }
            return existing;
        });

        int count = counter.count.incrementAndGet();

        if (count > rateLimit.maxRequests()) {
            log.warn("Rate limit exceeded: ip={} endpoint={} count={}/{}",
                    ip, endpoint, count, rateLimit.maxRequests());
            throw new RateLimitException();
        }

        log.debug("Rate check: ip={} endpoint={} count={}/{} window={}s",
                ip, endpoint, count, rateLimit.maxRequests(), rateLimit.windowSeconds());
    }

    private String buildEndpointKey(JoinPoint jp) {
        Method method = ((MethodSignature) jp.getSignature()).getMethod();
        return jp.getTarget().getClass().getSimpleName() + "." + method.getName();
    }

    // ── Inner value class ─────────────────────────────────────────────
    private static class WindowCounter {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger(0);
        WindowCounter(long windowStart) { this.windowStart = windowStart; }
    }
}
