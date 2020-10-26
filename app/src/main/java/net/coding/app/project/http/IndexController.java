package net.coding.app.project.http;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@Api(value = "项目首页", tags = "项目首页")
@Slf4j
@RestController
public class IndexController {

    @ApiOperation(value = "创建", notes = "创建公告,并且创建项目内冒泡")
    @GetMapping("/")
    public String findProjectResourcesList() {
        return "success";
    }
}
