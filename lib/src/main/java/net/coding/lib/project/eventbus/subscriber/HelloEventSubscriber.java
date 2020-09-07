package net.coding.lib.project.eventbus.subscriber;

import com.google.common.eventbus.Subscribe;

import net.coding.lib.project.entity.Hello;
import net.coding.shim.project.event.HelloEvent;
import net.coding.lib.project.service.HelloService;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * created by wang007 on 2020/7/26
 */
@Component
public class HelloEventSubscriber {

    @Resource
    private HelloService service;

    @Subscribe
    public void handle(HelloEvent event) {
        System.out.println("HelloEventSubscriber receive msg: " + event.getMsg());
        Hello hello = new Hello();
        BeanUtils.copyProperties(event,  hello);
        service.addHello(hello);
    }

}
