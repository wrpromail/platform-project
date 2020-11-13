package net.coding.lib.project.grpc.client;

import net.coding.common.rpc.client.EndpointGrpcClient;
import net.coding.e.proto.wiki.WikiProto;
import net.coding.e.proto.wiki.WikiServiceGrpc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WikiGrpcClient extends EndpointGrpcClient<WikiServiceGrpc.WikiServiceBlockingStub>{

    @Value("${grpc.client.wiki.serviceName:e-wiki}")
    private String serviceName;

    @Value("${grpc.client.wiki.servicePort:20153}")
    private int servicePort;

    @Override
    protected int provideServicePort() {
        return servicePort;
    }

    @Override
    protected String provideServiceName() {
        return serviceName;
    }

    public WikiProto.GetWikiByProjectIdAndIidData getWikiByProjectIdAndIidWithoutRecycleBin(Integer projectId, Integer iid) {
        WikiProto.GetWikiByProjectIdAndIidRequest request = WikiProto.GetWikiByProjectIdAndIidRequest.newBuilder()
                .setProjectId(projectId)
                .setIid(iid)
                .build();
        WikiProto.GetWikiByProjectIdAndIidResponse response = newStub().getWikiByProjectIdAndIidWithoutRecycleBin(request);
        if(response.getCode() == 0) {
            return response.getData();
        }
        return null;
    }

    public boolean wikiCanRead(Integer userId, Integer projectId, Integer wikiIid) {
        WikiProto.WikiCanReadRequest request = WikiProto.WikiCanReadRequest.newBuilder()
                .setProjectId(projectId)
                .setUserId(userId)
                .setWikiIid(wikiIid)
                .build();
        WikiProto.WikiCanReadResponse response = newStub().wikiCanRead(request);
        if(response.getCode() == 0) {
            return response.getData();
        }
        return false;
    }
}
