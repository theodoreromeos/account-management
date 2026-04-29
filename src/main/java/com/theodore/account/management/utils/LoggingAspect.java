package com.theodore.account.management.utils;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    @Value("${slow.threshold:2000}")
    private long slowThresholdMs;

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerLayer() {
    }

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceLayer() {
    }

    @Pointcut("target(org.springframework.data.repository.CrudRepository)")
    public void repositoryLayer() {
    }

    /**
     * Logs what endpoint is called
     */
    @Before("controllerLayer()")
    public void monitorController(JoinPoint joinPoint) {
        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        logger.info("{}.{}() called", className, methodName);
    }

    /**
     * If a service or database query takes too long it creates a warning log
     */
    @Around("serviceLayer() || repositoryLayer()")
    public Object monitor(ProceedingJoinPoint joinPoint) throws Throwable {

        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            if (duration > slowThresholdMs) {
                logger.warn("{}.{}() took {}ms [SLOW]", className, methodName, duration);
            }
        }

    }

}
