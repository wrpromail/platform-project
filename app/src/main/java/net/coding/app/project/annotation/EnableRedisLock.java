package net.coding.app.project.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EnableRedisLock {
    String lockKey();
    int waitTime() default 3;
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    int leaseTime() default 3;
}
