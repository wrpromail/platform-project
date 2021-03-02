package net.coding.lib.project.grpc.client;

import net.coding.common.rpc.client.EndpointGrpcClient;
import net.coding.e.proto.AgileTemplateProto;
import net.coding.e.proto.AgileTemplateServiceGrpc;
import net.coding.e.proto.CommonProto;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AgileTemplateGRpcClient extends EndpointGrpcClient<AgileTemplateServiceGrpc.AgileTemplateServiceBlockingStub> {

    @Value("${grpc.client.issue.serviceName:e-micro-agile}")
    private String serviceName;

    @Value("${grpc.client.issue.servicePort:20153}")
    private int servicePort;

    @Override
    protected int provideServicePort() {
        return servicePort;
    }

    @Override
    protected String provideServiceName() {
        return serviceName;
    }

    public void dataInitByProjectTemplate(Integer projectId, Integer userId, String projectTemplate, String template) {
        log.info("AgileTemplateGRpcClient.dataInitByProjectTemplate() " +
                        "projectId={}, userId={}, projectTemplate={}, template={}",
                projectId, userId, projectTemplate, template);
        AgileTemplateProto.TemplateInitRequest.Builder builder = AgileTemplateProto.TemplateInitRequest.newBuilder()
                .setProjectId(projectId)
                .setCreatorId(userId)
                .setProjectTemplate(projectTemplate);

        if (StringUtils.isNotBlank(template)) {
            builder.setTemplate(template);
        }
        AgileTemplateProto.TemplateInitRequest request = builder.build();
        CommonProto.Result result = newStub().dataInitByProjectTemplate(request);
        log.info("AgileTemplateGRpcClient.dataInitByProjectTemplate() response={}", result.toString());
        if (result == null || 0 != result.getCode()) {
            log.info("rpc dataInitByProjectTemplate error");
            if (result != null) {
                log.error("error msg = {}", result.getMessage());
            }
        }
    }
}
