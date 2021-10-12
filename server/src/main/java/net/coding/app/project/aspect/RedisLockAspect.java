package net.coding.app.project.aspect;

import net.coding.app.project.annotation.EnableRedisLock;
import net.coding.app.project.utils.RedissonLockUtil;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class RedisLockAspect {

    @Autowired
    private RedissonLockUtil redissonLockUtil;

    @Around(value = "@annotation(net.coding.app.project.annotation.EnableRedisLock)")
    public void handleRedisLock(ProceedingJoinPoint joinPoint) throws Throwable{
        EnableRedisLock redisLock = ((MethodSignature) joinPoint.getSignature())
                .getMethod()
                .getAnnotation(EnableRedisLock.class);
        String lockKey = redisLock.lockKey();
        int waitTime = redisLock.waitTime();
        TimeUnit timeUnit = redisLock.timeUnit();
        int leaseTime = redisLock.leaseTime();
        if(redissonLockUtil.tryLock(lockKey, timeUnit, waitTime, leaseTime)) {
            try {
                joinPoint.proceed();
                redissonLockUtil.unlock(lockKey);
            } catch (Exception ex) {
                redissonLockUtil.unlock(lockKey);
            }
        }
    }
}
