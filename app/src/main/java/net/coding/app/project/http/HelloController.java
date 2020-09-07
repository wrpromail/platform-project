package net.coding.app.project.http;

import com.google.common.eventbus.EventBus;

import net.coding.common.annotation.CsrfValidationSetting;
import net.coding.common.annotation.EnterpriseApiProtector;
import net.coding.common.annotation.ProjectApiProtector;
import net.coding.common.annotation.ProjectAuthToken;
import net.coding.common.annotation.ProtectedAPI;
import net.coding.common.annotation.enums.Action;
import net.coding.common.annotation.enums.Function;
import net.coding.common.constants.OAuthConstants;
import net.coding.common.constants.TwoFactorAuthConstants;
import net.coding.common.json.Json;
import net.coding.lib.project.entity.Hello;
import net.coding.shim.project.event.HelloEvent;
import net.coding.lib.project.service.HelloService;

import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import static net.coding.common.constants.DeployTokenScopeEnum.PROJECT_TWEET_RW;

/**
 * created by wang007 on 2020/7/26
 */
@RestController
@Slf4j
@RequestMapping("/act/hello")
public class HelloController {

    @Resource
    private HelloService helloService;

    @Resource
    private EventBus eventBus;

    /**
     * 测试 eventBus
     *
     * @param hello
     * @return
     */
    @PostMapping("/event/hello")
    public String hello(@RequestBody Hello hello) {
        if (hello == null) return "error";
        HelloEvent helloEvent = new HelloEvent();
        BeanUtils.copyProperties(hello, helloEvent);
        eventBus.post(helloEvent);
        return "ok";
    }


    @PostMapping("/addHello")
    public String addHello(@RequestBody Hello hello) {
        if (hello == null) return "error";
        helloService.addHello(hello);
        return "ok";
    }

    @GetMapping("/getHello")
    public String getHello(Integer id) {
        if (id == null) return "error";
        Hello byId = helloService.getById(id);
        return Json.toJson(byId);
    }

    /**
     * 1. 跳过 csrf 校验
     * 2. 不登录也能访问
     *
     * @return
     */

    @CsrfValidationSetting(skip = true)
    @ProtectedAPI(loginRequired = false)
    @GetMapping("/index")
    public String index() {
        return "index";
    }

    /**
     * 1. 用户需要有 企业 RBAC function = EnterpriseProject, action = Update 才能访问
     * 2. 开启两次密码验证
     *
     * @return
     */
    @EnterpriseApiProtector(function = Function.EnterpriseProject, action = Action.Update)
    @ProtectedAPI(authMethod = TwoFactorAuthConstants.AUTH_TYPE_PASSWORD)
    @GetMapping("/index2")
    public String index2() {
        return "index2";
    }

    /**
     *  1. 用户需要有 项目 RBAC function = ProjectCI, action = Update 才能访问
     *  2. oauth 或者 个人令牌访问时， scope = project
     *  3. 通过项目令牌 scope = project_tweet_rw, 也可以访问
     *
     * @return
     */
    @ProjectApiProtector(function = Function.ProjectCI, action = Action.Update)
    @ProtectedAPI(oauthScope = OAuthConstants.Scope.PROJECT)
    @ProjectAuthToken(scope = PROJECT_TWEET_RW)
    @GetMapping("/index3")
    public String index3() {
        return "index3";
    }


}
