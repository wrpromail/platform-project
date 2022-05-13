package net.coding.app.project.grpc;

import net.coding.lib.project.dao.SearchEntityDao;
import net.coding.lib.project.entity.SearchEntity;
import net.coding.proto.platform.project.SearchEntityProto;
import net.coding.proto.platform.project.SearchEntityServiceGrpc;

import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;

@Slf4j
@GRpcService
@AllArgsConstructor
public class SearchEntityGrpcService extends SearchEntityServiceGrpc.SearchEntityServiceImplBase {

    @Autowired
    RedissonClient redisson;

    @Autowired
    SearchEntityDao searchEntityDao;

    @Override
    public void insertOrUpdateSearchEntity(SearchEntityProto.SearchEntityRequest request, StreamObserver<SearchEntityProto.InsertOrUpdateSearchEntityResponse> responseObserver) {
        //request validate
        if (request.getProjectId() == 0
                || StringUtils.isBlank(request.getTargetType())
                || StringUtils.isBlank(request.getTitleKeywords())
                || request.getTargetId() == 0
                || request.getCreatorId() == 0
        ) {
            SearchEntityProto.InsertOrUpdateSearchEntityResponse response = SearchEntityProto.InsertOrUpdateSearchEntityResponse
                    .newBuilder()
                    .setCode(CodeProto.Code.INVALID_PARAMETER)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        final String lockName = "searchEntity:lock:targetId:%d:targetType:%s";

        RLock lock = redisson.getLock(String.format(lockName, request.getTargetId(), request.getTargetType()));
        try {
            lock.lock(10, TimeUnit.SECONDS);
            SearchEntity searchEntity = searchEntityDao.getByTargetIdAndType(request.getTargetId(), request.getTargetType());
            if (searchEntity == null) {
                searchEntity = new SearchEntity();
                searchEntity.setProjectId(request.getProjectId());
                searchEntity.setTargetType(request.getTargetType());
                searchEntity.setTargetId(request.getTargetId());
                searchEntity.setCreatorId(request.getCreatorId());
                searchEntity.setTitleKeywords(request.getTitleKeywords());
                searchEntity.setContentKeywords(request.getContentKeywords());
                searchEntityDao.insertEntry(searchEntity);
            } else {
                if (searchEntityDao.updateEntry(searchEntity.getId(), request.getTitleKeywords(), request.getContentKeywords()) > 0) {
                    searchEntity.setTitleKeywords(request.getTitleKeywords());
                    searchEntity.setContentKeywords(request.getContentKeywords());
                }
            }
            SearchEntityProto.InsertOrUpdateSearchEntityResponse response = SearchEntityProto.InsertOrUpdateSearchEntityResponse
                    .newBuilder()
                    .setCode(CodeProto.Code.SUCCESS)
                    .setSearchEntity(bean2Proto(searchEntity))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error(e.toString());
            SearchEntityProto.InsertOrUpdateSearchEntityResponse response = SearchEntityProto.InsertOrUpdateSearchEntityResponse
                    .newBuilder()
                    .setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage("INTERNAL ERROR")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void deleteSearchEntity(SearchEntityProto.SearchEntityRequest request, StreamObserver<SearchEntityProto.DeleteSearchEntityResponse> responseObserver) {
        //request validate
        if (StringUtils.isBlank(request.getTargetType())
                || request.getTargetId() == 0
        ) {
            SearchEntityProto.DeleteSearchEntityResponse response = SearchEntityProto.DeleteSearchEntityResponse
                    .newBuilder()
                    .setCode(CodeProto.Code.INVALID_PARAMETER)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        try {
            SearchEntity searchEntity = searchEntityDao.getByTargetIdAndType(request.getTargetId(), request.getTargetType());
            if (searchEntity != null) {
                searchEntityDao.deleteById(searchEntity.getId());
            }
            SearchEntityProto.DeleteSearchEntityResponse response = SearchEntityProto.DeleteSearchEntityResponse
                    .newBuilder()
                    .setCode(CodeProto.Code.SUCCESS)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error(e.toString());
            SearchEntityProto.DeleteSearchEntityResponse response = SearchEntityProto.DeleteSearchEntityResponse
                    .newBuilder()
                    .setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage("INTERNAL ERROR")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

    }

    @Override
    public void batchUpdateTargetTypeByTargetIdAndType(SearchEntityProto.UpdateSearchTargetTypeByTargetIdAndTypeRequest request, StreamObserver<SearchEntityProto.UpdateSearchTargetTypeByTargetIdAndTypeResponse> responseObserver) {
        List<Integer> targetIdList = request.getTargetIdList();
        String newTargetType = request.getNewTargetType();
        Integer projectId = request.getProjectId();
        String targetType = request.getTargetType();
        if (targetIdList.isEmpty()
                || StringUtils.isBlank(newTargetType)
                || projectId == 0
                || StringUtils.isBlank(targetType)
        ) {
            SearchEntityProto.UpdateSearchTargetTypeByTargetIdAndTypeResponse response = SearchEntityProto.UpdateSearchTargetTypeByTargetIdAndTypeResponse
                    .newBuilder()
                    .setCode(CodeProto.Code.INVALID_PARAMETER)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        try {
            searchEntityDao.batchUpdateTargetTypeByTargetIdAndType(newTargetType, projectId, targetType, targetIdList);
            SearchEntityProto.UpdateSearchTargetTypeByTargetIdAndTypeResponse response = SearchEntityProto.UpdateSearchTargetTypeByTargetIdAndTypeResponse
                    .newBuilder()
                    .setCode(CodeProto.Code.SUCCESS)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error(e.toString());
            SearchEntityProto.UpdateSearchTargetTypeByTargetIdAndTypeResponse response = SearchEntityProto.UpdateSearchTargetTypeByTargetIdAndTypeResponse
                    .newBuilder()
                    .setCode(CodeProto.Code.INTERNAL_ERROR)
                    .setMessage("INTERNAL ERROR")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    private SearchEntityProto.SearchEntity bean2Proto(SearchEntity bean) {
        return SearchEntityProto.SearchEntity.newBuilder()
                .setId(bean.getId())
                .setProjectId(bean.getProjectId())
                .setTargetId(bean.getTargetId())
                .setTargetType(bean.getTargetType())
                .setCreatorId(bean.getCreatorId())
                .setTitleKeywords(bean.getTitleKeywords())
                .setContentKeywords(bean.getContentKeywords())
                .build();
    }
}
