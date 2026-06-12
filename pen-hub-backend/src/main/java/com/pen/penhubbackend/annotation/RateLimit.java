package com.pen.penhubbackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 * 基于 Redis + 固定窗口计数器实现
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 时间窗口内的最大请求数
     */
    int maxRequests() default 60;

    /**
     * 时间窗口（秒）
     */
    int windowSeconds() default 60;

    /**
     * 限流 key 前缀（默认使用方法全限定名）
     */
    String keyPrefix() default "";
}
