package net.coding.app.project.http;

import net.coding.app.project.metric.MetricsProvider;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class IndexController {
    @GetMapping("/")
    public String findProjectResourcesList() {
        return "success";
    }
}
