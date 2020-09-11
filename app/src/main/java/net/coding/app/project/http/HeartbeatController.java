package net.coding.app.project.http;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * 心跳检查
 */
@RestController
@Slf4j
@RequestMapping("/heartbeat")
public class HeartbeatController {

    @GetMapping("/ping")
    public String findProjectResourcesList() {
        return "pong";
    }
}
