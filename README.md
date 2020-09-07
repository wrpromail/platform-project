# platform-starter 微服务骨架项目

## 具体代码可参考本项目

## 开发步骤 
1. 在 lib 模块下，完成对业务代码的开发。
2. 在 [proto 项目](https://codingcorp.coding.net/p/coding-dev/d/proto/git) 定义要发布服务的 grpc proto。
3. 1 和 2 的顺序可随意调换。
4. 完成 app / client 模块的编写。
5. 通过 CI 构建构建微服务和对应的 client。
6. 通过 CI 手动构建指定日期的版本作为稳定版本发布（不稳定版本可使用 test 后缀，例如 20200725.test1，20200725.test2等）。

## 简介
> 微服务骨架项目，满足创建微服务所需基础功能的示例代码，包括
  1. 项目结构
  2. 项目依赖
  2. http server 和 鉴权相关的功能
  3. grpc 服务
  4. 分布式 EventBus
  5. CI ，自动生成 k8s yaml 文件
  
### 项目结构
> 整个微服务项目主要包括 4 个模块，其中 shim 模块不是必须的

1. app: 提供对外暴露 http 和 grpc 服务
   * 模块依赖: lib,shim
   * 模块严禁依赖: client
   
2. client: 提供本服务的 grpc client
   * 模块依赖: lib,shim
   * 模块严禁依赖: client

3. lib: 本服务的业务代码
   * 模块依赖: shim
   * 模块严禁依赖: client, app(反向依赖)
    
4. shim: 中间层，防止 client 直接依赖 lib。如果 client 不需要依赖的话，可以不需要 shim 
> 主要是防止 client 直接依赖 lib。

```
例1：
UserDTO 这种数据承载对象，在 lib 业务代码中需要用到，同时 client 也需要用到。 有两种错误的解决办法:
1. copy 这个 UserDTO 到 client 上。 但是这种 逻辑 同样逻辑代码 copy 的到处都是，这是非常糟糕和不可接受的。
2. 直接依赖 lib 业务代码。这种情况更糟糕，client 非常重不说，还把具体服务的业务代码实现依赖到 client，最终集成到其他服务中。


例2：
eventbus 需要将 event 发布出去。 
1. 在 lib 中写 eventSubscriber 时会用到 event
2. 通过 client 把 event 发布出去需要用到 event。
3. 所以需要将 event 放到 shim 模块上，给 client 和 lib 模块用。


基于以上情况，提出 shim 中间层。例如 上例中提到的 UserDTO，在 client 和 lib 都需要用到的情况下，就放到 shim 模块中。
数据库实体是强制需要放到 lib 模块的。
其他 utils 等工具同样需求也是如此。

```


```
.
├── app                             ---> 应用启动模块，对外暴露 http 和 grpc 接口
│   └── net.coding.app.xxx          ---> 项目包结构，xxx 是对应的服务名
│       ├── http                    ---> 放 http controller 
│       ├── grpc                    ---> 放 grpc 服务
│       ├── config                  ---> 放 http controller 和 grpc 服务配置相关的类
│       ├── Application.java        ---> 应用启动类  
│       ├── XxxGrpcServiceRegister.java  ---> grpc 服务注册到 etcd 上
│       └── ...                     
│
├── client                      ---> client，主要是 grpc client，eventbus 中 event，未来也有可能包含其他client 功能
│   └── net.coding.client.xxx   ---> 项目包结构，xxx 是对应的服务名
│       ├── grpc                ---> 放 grpc client
│       ├── Config.java         ---> 配置类，用于其他服务 Import 引用
│       └── ...                 ---> 其他模块代码    
│
├── lib                     ---> 业务代码
│   └── net.coding.lib.xxx  ---> 项目包结构，xxx 是对应的服务名
│       ├── service         ---> service
│       ├── dao             ---> dao
│       ├── bean            ---> 数据库实体 bean
│       ├── Config.java     ---> 配置类，用于其他服务 Import 引用
│       └── ...             
│
└── shim                    ---> 承接 业务代码 lib 和 client 的缓冲模块
    └── net.coding.shim.xxx ---> 项目包结构，xxx 是对应的服务名    
        ├── dto             ---> 数据传输对象    
        ├── utils           ---> 工具类
        ├── Config.java     ---> 配置类，用于其他服务 Import 引用
        └── ...             
```

### 项目依赖
* 基础模块依赖（common 相关模块的依赖）
    1. server: http server 
    2. eventbus: 分布式 eventbus
    3. rpc: 提供 grpc 服务注册和调用的工具
    4. db 和 dbclient: 数据操作的工具
    5. tracing: 分布式透传库
    6. rabbitmq: 项目有使用 rabbitmq，可使用该类库
    7. 项目的基础依赖主要是这些，更多依赖可参考 [common 项目](https://codingcorp.coding.net/p/coding-dev/d/common/git)

* proto 依赖, 参考 [proto 项目](https://codingcorp.coding.net/p/coding-dev/d/proto/git)
    1. 在 proto 项目中定义 grpc proto，然后到自己的微服务中引入。
    2. 基于 proto 编写 grpc server 和 grpc client
    

### http server 和 鉴权相关的功能
1. http server 用法跟 coding-dev。
2. 鉴权是通过注解完成的，配合自动生成 k8s yaml 文件把权限信息生成到 deployment.yaml（deployment.yaml 包含 apiRoute 和 apiAuth），
最后部署到 k8s 时，网关自动读取权限信息控制权限。

### grpc 服务
1. 使用 rpc 包中的 [GrpcServiceRegister](https://codingcorp.coding.net/p/coding-dev/d/common/git/tree/master/rpc/src/main/java/net/coding/common/rpc/GrpcServiceRegister.java) 
封装了注册服务到 etcd 的类。 犹如现阶段有些服务还在使用 etcd 调用，所以还是得注册服务到 etcd 中，后续完全切换到 k8s service 调用时，再废掉 etcd。
   
### grpc client
1. [EndpointGrpcClient](https://codingcorp.coding.net/p/coding-dev/d/common/git/tree/master/rpc/src/main/java/net/coding/common/rpc/client/EndpointGrpcClient.java)
封装了 grpc client 快速创建 Stub 方法，其中还创建 Channel idle 和调用重试。
2. 通过 grpc.client.enabledEtcdDiscovery 变量环境控制 k8s / etcd 调用。 
    1. true: eted 方式调用服务。
    2. false: k8s service 方式调用服务。
    3. 默认值: false, 即 k8s service 方式调用服务。
    
3. 必须提供 client 服务名和端口的环境变量，方便使用方替换。（主要是线上环境的服务名往往跟开发环境是不同的）


### 分布式 EventBus
1. 基于 rabbitmq 封装了跟原来 guava EventBus 一样的 api（继承了 guava EventBus）所以使用方式跟原来一样，
eventbus 包完成具体实例的替换。


###  CI 自动生成 k8s yaml 文件
```
  task createYamlTask(group: 'kubernetes', type: Exec) {
      dependsOn bootRepackage
      File file = getTemporaryDir();
      commandLine "java", "-jar", jar.getArchivePath(), project.getName(), file.parentFile.parentFile.path + "/kubernetes"
   }
```
1. 通过 task 自动生成 k8s yaml 文件（执行 gradle task  kubernetes/createYamlTask），生成到 当前项目下的 deploy 目录中，
然后把生成 yaml 文件，copy 到 deploy 项目进行发布。

2. 检查生成的 yaml 文件是否满足自己的需求。若不满足，可自行修改。


