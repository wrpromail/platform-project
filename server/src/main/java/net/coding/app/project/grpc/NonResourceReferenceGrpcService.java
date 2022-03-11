package net.coding.app.project.grpc;

import net.coding.app.project.utils.GrpcUtil;
import net.coding.lib.project.dao.NonResourceReferenceDao;
import net.coding.lib.project.entity.NonResourceReference;
import net.coding.proto.platform.project.NonResourceReferenceProto;
import net.coding.proto.platform.project.NonResourceReferenceServiceGrpc;

import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;

@Slf4j
@GRpcService
public class NonResourceReferenceGrpcService extends NonResourceReferenceServiceGrpc.NonResourceReferenceServiceImplBase {

    @Autowired
    NonResourceReferenceDao resourceReferenceDao;

    @Override
    public void countByTarget(NonResourceReferenceProto.CountNoneResourceByTargetRequest request, StreamObserver<NonResourceReferenceProto.CountNoneResourceByTargetResponse> responseObserver) {
        try {
            Integer count = resourceReferenceDao.countByTarget(request.getTargetProjectId(), request.getTargetIid());
            NonResourceReferenceProto.CountNoneResourceByTargetResponse response = NonResourceReferenceProto.CountNoneResourceByTargetResponse
                    .newBuilder()
                    .setCode(CodeProto.Code.SUCCESS)
                    .setCount(count)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error(e.toString());
            NonResourceReferenceProto.CountNoneResourceByTargetResponse response = NonResourceReferenceProto.CountNoneResourceByTargetResponse
                    .newBuilder()
                    .setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage("INTERNAL ERROR")
                    .setCount(0)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void addNoneResourceReference(NonResourceReferenceProto.AddNonResourceReferenceRequest request, StreamObserver<NonResourceReferenceProto.AddNonResourceReferenceResponse> responseObserver) {
        try {
            //request validate
            if (request.getSelfId() == 0
                    || request.getSelfProjectId() == 0
                    || request.getTargetId() == 0
                    || request.getTargetCode() == 0
                    || request.getTargetProjectId() == 0
                    || StringUtils.isBlank(request.getSelfType())
                    || StringUtils.isBlank(request.getTargetType())) {
                NonResourceReferenceProto.AddNonResourceReferenceResponse response = NonResourceReferenceProto.AddNonResourceReferenceResponse
                        .newBuilder()
                        .setCode(CodeProto.Code.INVALID_PARAMETER)
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }
            NonResourceReference nonResourceReference = new NonResourceReference();
            nonResourceReference.setSelfId(request.getSelfId());
            nonResourceReference.setSelfProjectId(request.getSelfProjectId());
            nonResourceReference.setSelfType(request.getSelfType());
            nonResourceReference.setSelfContent(request.getSelfContent());
            nonResourceReference.setTargetId(request.getTargetId());
            nonResourceReference.setTargetProjectId(request.getTargetProjectId());
            nonResourceReference.setTargetIid(request.getTargetCode());
            nonResourceReference.setTargetType(request.getTargetType());

            resourceReferenceDao.addNoneResourceReference(nonResourceReference);
            NonResourceReferenceProto.AddNonResourceReferenceResponse response = NonResourceReferenceProto.AddNonResourceReferenceResponse
                    .newBuilder()
                    .setCode(CodeProto.Code.SUCCESS)
                    .setNonResourceReference(
                            GrpcUtil.nonResourceReferenceBean2Proto(nonResourceReference)
                    )
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error(e.toString());
            NonResourceReferenceProto.AddNonResourceReferenceResponse response = NonResourceReferenceProto.AddNonResourceReferenceResponse
                    .newBuilder()
                    .setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage("INTERNAL ERROR")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
