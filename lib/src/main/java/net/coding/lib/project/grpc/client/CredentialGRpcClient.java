package net.coding.lib.project.grpc.client;

import net.coding.common.rpc.client.EndpointGrpcClient;
import net.coding.e.proto.CommonProto;
import net.coding.lib.project.parameter.BaseCredentialParameter;
import net.coding.proto.CredentialProto;
import net.coding.proto.CredentialServiceGrpc;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CredentialGRpcClient extends EndpointGrpcClient<CredentialServiceGrpc.CredentialServiceBlockingStub> {

    @Value("${grpc.client.ci.manager.serviceName:e-ci-manager}")
    private String serviceName;

    @Value("${grpc.client.ci.manager.servicePort:20153}")
    private int servicePort;

    @Override
    protected int provideServicePort() {
        return servicePort;
    }

    @Override
    protected String provideServiceName() {
        return serviceName;
    }

    public Integer createCredential(String userGk, boolean encrypt, BaseCredentialParameter parameter) {
        log.info("CredentialGRpcClient.createCredential() userGk={}, encrypt={}", userGk, encrypt);
        CredentialProto.CreateCredentialRequest request = CredentialProto.CreateCredentialRequest.newBuilder()
                .setUserGk(userGk)
                .setEncrypt(encrypt)
                .setForm(CredentialProto.CredentialForm.newBuilder()
                        .setType(CredentialProto.CredentialType.valueOf(parameter.getType()))
                        .setScope(CredentialProto.CredentialScope.forNumber(parameter.getScope()))
                        .setGeneratedBy(CredentialProto.ConnGenerateBy.valueOf(parameter.getConnGenerateBy().name()))
                        .setId(parameter.getId())
                        .setTeamId(parameter.getTeamId())
                        .setProjectId(parameter.getProjectId())
                        .setCreatorId(parameter.getCreatorId())
                        .setCredentialId(parameter.getCredentialId())
                        .setName(parameter.getName())
                        .setDescription(parameter.getDescription())
                        .setAllSelect(parameter.isAllSelect())
                        .build())
                .build();
        CommonProto.Result result = newStub().createCredential(request);
        log.info("CredentialGRpcClient.createCredential() response={}", result.toString());
        if (result.getCode() == 0) {
            return result.getId();
        }
        return null;
    }

    public CredentialProto.Credential getById(Integer id,boolean decrypt) {
        log.info("CredentialGRpcClient.getById() id={}", id);
        CredentialProto.GetByIdRequest request = CredentialProto.GetByIdRequest.newBuilder()
                .setId(id)
                .setDecrypt(decrypt)
                .build();
        CredentialProto.GetCredentialResponse response = newStub().getById(request);
        log.info("CredentialGRpcClient.getById() response={}", response.toString());
        if (response.getCode() == 0) {
            return response.getData();
        }
        return null;
    }


}
