package net.coding.app.project.task;

import net.coding.client.project.CodingProjectResourceGrpcClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FixProjectResourceUrlTask {

    @Autowired
    private CodingProjectResourceGrpcClient codingProjectResourceGrpcClient;

    @Scheduled(cron = "0 0/1 *  * * ?")  //每天零点执行
    public void printDate(){
        String url = codingProjectResourceGrpcClient.getResourceLink(42);
        System.out.println("--------#####----------" + url);
        System.out.println("--------#####----------" + new Date().toString());
    }
}
