package com.akash.pooler_backend.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Cross-cutting concern: method-level timing for all service classes.
 * Logs WARN when any service method exceeds 500ms.
 */
@Slf4j
@Aspect
@Component
public class ServiceLoggingAspect {

    @Pointcut("execution(* com.enterprise.auth.service.impl.*.*(..))")
    public void serviceMethods() {}

    @Around("serviceMethods()")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String method = pjp.getSignature().toShortString();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > 500) {
                log.warn("SLOW SERVICE [{}] took {}ms", method, elapsed);
            } else {
                log.debug("SERVICE [{}] completed in {}ms", method, elapsed);
            }
            return result;
        } catch (Throwable t) {
            log.error("SERVICE [{}] threw {} in {}ms", method, t.getClass().getSimpleName(),
                    System.currentTimeMillis() - start);
            throw t;
        }
    }
}
