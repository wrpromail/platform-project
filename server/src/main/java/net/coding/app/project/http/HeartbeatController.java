package net.coding.app.project.http;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳检查
 */
@RestController
@Slf4j
@Api(value = "心跳检查", tags = "心跳检查")
@RequestMapping("/api/platform/project")
public class HeartbeatController {

    @GetMapping("/ping")
    public String findProjectResourcesList() {
        return "pong";
    }
}
