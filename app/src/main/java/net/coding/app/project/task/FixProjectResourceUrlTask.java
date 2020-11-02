package net.coding.app.project.task;

import net.coding.lib.project.utils.RedisUtil;
import net.coding.app.project.utils.RedissonLockUtil;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.grpc.client.CodingProjectResourceGrpcClient;
import net.coding.lib.project.service.ProjectResourceService;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FixProjectResourceUrlTask {

    @Resource
    private CodingProjectResourceGrpcClient codingProjectResourceGrpcClient;

    @Resource
    private ProjectResourceService projectResourceService;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RedissonLockUtil redissonLockUtil;

    //@Scheduled(cron = "0 30 11 * * ?")  //每天1点执行
    public void fixUrl() {
        log.info("FixProjectResourceUrlTask beginTime={}", System.currentTimeMillis());
        String lockKey = "fixProjectResourceUrlTaskForFixUrl";
        try {
            if(!redissonLockUtil.tryLock(lockKey, TimeUnit.SECONDS, 3, 36000)) {
                return;
            }
            log.info("FixProjectResourceUrlTask get lock success");
            boolean taskFlag = true;
            String key = "FixProjectResourceIdValueForResourceUrl";
            Integer id = 0;
            if (redisUtil.exists(key)) {
                id = Integer.valueOf(redisUtil.get(key));
            } else {
                id = projectResourceService.getBeginFixId() - 1;
                redisUtil.set(key, id);
            }
            ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 8, 5000,
                    TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(32));
            while (taskFlag) {
                log.info("FixProjectResourceUrlTask id={}", id);
                List<Integer> projectResourceIdList = projectResourceService.findFixResourceList(id);
                //if (CollectionUtils.isEmpty(projectResourceIdList) || DateUtil.getCurrentHour().compareTo(9) >= 0) {
                if (CollectionUtils.isEmpty(projectResourceIdList)) {
                    taskFlag = false;
                } else {
                    log.info("FixProjectResourceUrlTask projectResourceIdList.size()={}", projectResourceIdList.size());
                    final CountDownLatch latch = new CountDownLatch(projectResourceIdList.size());
                    executor.execute(() -> {
                        for (Integer value : projectResourceIdList) {
                            String url = codingProjectResourceGrpcClient.getResourceLink(value);
                            log.info("FixProjectResourceUrlTask value={}, url={}", value, url);
                            if (!StringUtils.isEmpty(url)) {
                                try {
                                    ProjectResource projectResource = new ProjectResource();
                                    projectResource.setId(value);
                                    projectResource.setResourceUrl(url);
                                    projectResourceService.update(projectResource);
                                } catch (Exception ex) {
                                    log.error("FixProjectResourceUrlTask update exception={}", ex);
                                }
                            }
                            latch.countDown();
                        }
                    });
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    id = Collections.max(projectResourceIdList);
                    redisUtil.set(key, id);
                }
            }
            redissonLockUtil.unlock(lockKey);
        } catch (Exception ex) {
            log.error("FixProjectResourceUrlTask exception={}", ex);
            redissonLockUtil.unlock(lockKey);
        } finally {
            log.info("FixProjectResourceUrlTask endTime={}", System.currentTimeMillis());
        }
    }
}
