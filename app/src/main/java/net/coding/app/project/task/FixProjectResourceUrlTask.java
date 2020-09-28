package net.coding.app.project.task;

import net.coding.app.project.utils.RedisUtil;
import net.coding.client.project.CodingProjectResourceGrpcClient;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.service.ProjectResourceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FixProjectResourceUrlTask {

    @Autowired
    private CodingProjectResourceGrpcClient codingProjectResourceGrpcClient;

    @Autowired
    private ProjectResourceService projectResourceService;

    @Resource
    private RedisUtil redisUtil;

    @Scheduled(cron = "0 0 1 * * ?")  //每天1点执行
    public void fixUrl() {
        log.info("FixProjectResourceUrlTask beginTime={}", System.currentTimeMillis());
        try {
            boolean taskFlag = true;
            String key = "FixProjectResourceUrlTaskId";
            Integer id = 0;
            if (redisUtil.exists(key)) {
                id = Integer.valueOf(redisUtil.get(key));
            } else {
                id = projectResourceService.getBeginFixId() - 1;
                redisUtil.set(key, id);
            }
            ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 8, 2000,
                    TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(32));
            while (taskFlag) {
                log.info("FixProjectResourceUrlTask id={}, beginFindFixResourceListTime={}", id, System.currentTimeMillis());
                List<Integer> projectResourceIdList = projectResourceService.findFixResourceList(id);
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
                                ProjectResource projectResource = new ProjectResource();
                                projectResource.setId(value);
                                projectResource.setResourceUrl(url);
                                log.info("FixProjectResourceUrlTask projectResource={}", projectResource.toString());
                                projectResourceService.update(projectResource);
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
                log.info("FixProjectResourceUrlTask endFixResourceUrlTime={}", System.currentTimeMillis());
            }
        } catch (Exception ex) {
            log.error("FixProjectResourceUrlTask fixUrl exception={}", ex);
        } finally {
            log.info("FixProjectResourceUrlTask endTime={}", System.currentTimeMillis());
        }
    }
}
