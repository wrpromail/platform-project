package net.coding.app.project;

import net.coding.common.rpc.GrpcServiceRegister;

import org.springframework.stereotype.Component;

/**
 * 注册 starter 服务到 etcd 上
 *
 * created by wang007 on 2020/7/26
 */
@Component
public class GrpcServerRegister extends GrpcServiceRegister {

    @Override
    protected String provideServiceName() {
        return "project";
    }

}
