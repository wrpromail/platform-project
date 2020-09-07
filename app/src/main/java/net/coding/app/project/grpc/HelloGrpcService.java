package net.coding.app.project.grpc;

import net.coding.lib.project.entity.Hello;
import net.coding.lib.project.service.HelloService;

import org.lognet.springboot.grpc.GRpcService;

import javax.annotation.Resource;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import proto.hello.HelloProto;
import proto.hello.HelloServiceGrpc;

/**
 * created by wang007 on 2020/7/26
 */
@Slf4j
@GRpcService
public class HelloGrpcService extends HelloServiceGrpc.HelloServiceImplBase {

    @Resource
    private HelloService helloService;

    @Override
    public void hello(HelloProto.HelloRequest request, StreamObserver<HelloProto.HelloResponse> responseObserver) {
        String msg = request.getMsg();
        log.info("hello grpc service receive: " + msg);
        HelloProto.HelloResponse build = HelloProto.HelloResponse.newBuilder().setId(0).setMsg("hello client").build();
        responseObserver.onNext(build);
        responseObserver.onCompleted();
    }

    @Override
    public void addHello(HelloProto.HelloRequest request, StreamObserver<HelloProto.HelloResponse> responseObserver) {
        Hello hello = new Hello();
        hello.setMsg(request.getMsg());
        helloService.addHello(hello);

        HelloProto.HelloResponse build = HelloProto.HelloResponse.newBuilder()
                .setId(hello.getId() == null ? 0 : hello.getId())
                .setMsg(hello.getMsg())
                .build();
        responseObserver.onNext(build);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteHello(HelloProto.DeleteHelloRequest request, StreamObserver<HelloProto.HelloResponse> responseObserver) {
        Hello hello = helloService.getById(request.getId());
        helloService.deleteById(hello);
        HelloProto.HelloResponse build = HelloProto.HelloResponse.newBuilder()
                .setId(hello.getId())
                .setMsg(hello.getMsg())
                .build();
        responseObserver.onNext(build);
        responseObserver.onCompleted();
    }

    @Override
    public void updateHello(HelloProto.UpdateHelloRequest request, StreamObserver<HelloProto.HelloResponse> responseObserver) {
        Hello hello = new Hello();
        hello.setId(request.getId());
        hello.setMsg(request.getMsg());
        helloService.updateById(hello);
        HelloProto.HelloResponse build = HelloProto.HelloResponse.newBuilder()
                .setId(hello.getId())
                .setMsg(hello.getMsg())
                .build();
        responseObserver.onNext(build);
        responseObserver.onCompleted();
    }

    @Override
    public void getHello(HelloProto.GetHelloRequest request, StreamObserver<HelloProto.HelloResponse> responseObserver) {
        Hello hello = helloService.getById(request.getId());
        HelloProto.HelloResponse build = HelloProto.HelloResponse.newBuilder()
                .setId(hello.getId())
                .setMsg(hello.getMsg())
                .build();
        responseObserver.onNext(build);
        responseObserver.onCompleted();
    }
}
