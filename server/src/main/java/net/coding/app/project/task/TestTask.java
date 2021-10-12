package net.coding.app.project.task;

import org.springframework.stereotype.Component;

@Component
public class TestTask {

//    @Scheduled(cron = "0 0/2 * * * ?")  //每天1点执行
//    @EnableRedisLock(lockKey = "TestTaskLockKey")
    public void test() {
        System.out.println("-----------###------------");
        System.out.println(System.currentTimeMillis());
    }
}
