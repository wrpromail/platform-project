package net.coding.app.project.task;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TestJobHandler {

    @XxlJob("demoJobHandler")
    public ReturnT<String> demoJobHandler(String param) throws Exception {
        //XxlJobLogger.log("param["+ param +"] invalid.");
        System.out.println("-------######-------" + System.currentTimeMillis());
        return ReturnT.SUCCESS;
    }
}
