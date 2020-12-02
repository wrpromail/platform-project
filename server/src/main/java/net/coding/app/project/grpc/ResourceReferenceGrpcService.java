package net.coding.app.project.grpc;

import net.coding.app.project.utils.GrpcUtil;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.entity.ResourceReference;
import net.coding.lib.project.service.ProjectResourceService;
import net.coding.lib.project.service.ResourceReferenceService;
import net.coding.lib.project.utils.DateUtil;
import net.coding.proto.platform.project.ResourceReferenceProto;
import net.coding.proto.platform.project.ResourceReferenceServiceGrpc;

import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import proto.common.CodeProto;

@Slf4j
@GRpcService
public class ResourceReferenceGrpcService extends ResourceReferenceServiceGrpc.ResourceReferenceServiceImplBase {

    @Autowired
    private ResourceReferenceService resourceReferenceService;

    @Autowired
    private ProjectResourceService projectResourceService;

    @Override
    public void simpleAddResourceReference(ResourceReferenceProto.SimpleAddResourceReferenceRequest request,
                                           StreamObserver<ResourceReferenceProto.ResourceReferenceResponse> response) {
        try {
            log.info("simpleAddResourceReference() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getProjectId() <= 0 || request.getSelfIid() <= 0 || request.getTargetProjectId() <= 0 || request.getTargetIid() <= 0) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INVALID_PARAMETER, "simpleAddResourceReference parameters error", null, response);
                return;
            }
            ResourceReference record = resourceReferenceService.getByProjectIdAndCode(request.getProjectId(), request.getSelfIid(), request.getTargetProjectId(), request.getTargetIid());
            if (Objects.nonNull(record)) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INVALID_PARAMETER, "simpleAddResourceReference ResourceReference is empty", null, response);
                return;
            }
            if (Objects.equals(request.getProjectId(), request.getTargetProjectId()) && Objects.equals(request.getSelfIid(), request.getTargetIid())) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INVALID_PARAMETER, "simpleAddResourceReference reference cannot add self", null, response);
                return;
            }
            ProjectResource self = projectResourceService.getProjectResourceWithDeleted(request.getProjectId(), request.getSelfIid());
            ProjectResource target = projectResourceService.getProjectResourceWithDeleted(request.getTargetProjectId(), request.getTargetIid());
            if (Objects.isNull(self) || Objects.isNull(target)) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INVALID_PARAMETER, "simpleAddResourceReference ResourceReference is empty", null, response);
                return;
            }
            ResourceReference insert = new ResourceReference();
            insert.setSelfId(self.getTargetId());
            insert.setSelfIid(self.getCode());
            insert.setSelfType(self.getTargetType());
            insert.setSelfProjectId(self.getProjectId());
            insert.setTargetId(target.getTargetId());
            insert.setTargetIid(target.getCode());
            insert.setTargetType(target.getTargetType());
            insert.setTargetProjectId(target.getProjectId());
            insert.setCreatedAt(DateUtil.getCurrentDate());
            insert.setUpdatedAt(DateUtil.getCurrentDate());
            log.info("simpleAddResourceReference() insert: {}", insert.toString());
            ResourceReference resourceReference = resourceReferenceService.insert(insert);
            if (Objects.nonNull(resourceReference.getId()) && 0 < resourceReference.getId()) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.SUCCESS, "add success", GrpcUtil.getResourceReference(resourceReference), response);
            } else {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INTERNAL_ERROR, "add error", null, response);
            }
        } catch (Exception ex) {
            log.error("simpleAddResourceReference() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.resourceReferenceResponse(CodeProto.Code.INTERNAL_ERROR, "add error", null, response);
        }
    }

    @Override
    public void addResourceReference(ResourceReferenceProto.AddResourceReferenceRequest request,
                                     StreamObserver<ResourceReferenceProto.ResourceReferenceResponse> response) {
        try {
            log.info("addResourceReference() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getSelfIid() <= 0 || request.getSelfIid() <= 0 || request.getTargetProjectId() <= 0 || request.getTargetIid() <= 0) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INVALID_PARAMETER, "addResourceReference parameters error", null, response);
                return;
            }
            ResourceReference insert = new ResourceReference();
            insert.setSelfId(request.getSelfId());
            insert.setSelfIid(request.getSelfIid());
            insert.setSelfType(request.getSelfType());
            insert.setSelfProjectId(request.getSelfProjectId());
            insert.setTargetId(request.getTargetId());
            insert.setTargetIid(request.getTargetIid());
            insert.setTargetType(request.getTargetType());
            insert.setTargetProjectId(request.getTargetProjectId());
            insert.setCreatedAt(DateUtil.getCurrentDate());
            insert.setUpdatedAt(DateUtil.getCurrentDate());
            log.info("addResourceReference() insert: {}", insert.toString());
            ResourceReference resourceReference = resourceReferenceService.insert(insert);
            if (Objects.nonNull(resourceReference.getId()) && 0 < resourceReference.getId()) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.SUCCESS, "simpleAdd success", GrpcUtil.getResourceReference(resourceReference), response);
            } else {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INTERNAL_ERROR, "simpleAdd error", null, response);
            }
        } catch (Exception ex) {
            log.error("addResourceReference() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.resourceReferenceResponse(CodeProto.Code.INTERNAL_ERROR, "add error", null, response);
        }
    }

    @Override
    public void batchAddResourceReference(ResourceReferenceProto.BatchAddResourceReferenceRequest request,
                                          StreamObserver<ResourceReferenceProto.ResourceReferenceCommonResponse> response) {
        try {
            log.info("batchAddResourceReference() grpc service receive: {}", request != null ? request.toString() : "");
            if (CollectionUtils.isEmpty(request.getAddResourceReferenceRequestList())) {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "batchAddResourceReference param error", response);
                return;
            }
            List<ResourceReference> resourceReferenceList = new ArrayList<>();
            request.getAddResourceReferenceRequestList().forEach(resourceReference -> {
                ResourceReference insert = new ResourceReference();
                insert.setSelfId(resourceReference.getSelfId());
                insert.setSelfIid(resourceReference.getSelfIid());
                insert.setSelfType(resourceReference.getSelfType());
                insert.setSelfProjectId(resourceReference.getSelfProjectId());
                insert.setTargetId(resourceReference.getTargetId());
                insert.setTargetIid(resourceReference.getTargetIid());
                insert.setTargetType(resourceReference.getTargetType());
                insert.setTargetProjectId(resourceReference.getTargetProjectId());
                insert.setCreatedAt(DateUtil.getCurrentDate());
                insert.setUpdatedAt(DateUtil.getCurrentDate());
                resourceReferenceList.add(insert);
            });
            int batchInsertFlag = resourceReferenceService.batchInsert(resourceReferenceList);
            if (0 < batchInsertFlag) {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.SUCCESS, "batchAdd success", response);
            } else {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "batchAdd error", response);
            }
        }  catch (Exception ex) {
            log.error("batchAddResourceReference() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "batchAdd error", response);
        }
    }

    @Override
    public void updateResourceReference(ResourceReferenceProto.UpdateResourceReferenceRequest request,
                                     StreamObserver<ResourceReferenceProto.ResourceReferenceResponse> response) {
        try {
            log.info("updateResourceReference() grpc service receive: {}", request != null ? request.toString() : "");
            if(request.getSelfIid() <= 0 || request.getSelfIid() <= 0 || request.getTargetProjectId() <= 0 || request.getTargetIid() <= 0) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INVALID_PARAMETER, "updateResourceReference parameters error", null, response);
                return;
            }
            ResourceReference update = new ResourceReference();
            update.setSelfId(request.getSelfId());
            update.setSelfIid(request.getSelfIid());
            update.setSelfType(request.getSelfType());
            update.setSelfProjectId(request.getSelfProjectId());
            update.setTargetId(request.getTargetId());
            update.setTargetIid(request.getTargetIid());
            update.setTargetType(request.getTargetType());
            update.setTargetProjectId(request.getTargetProjectId());
            update.setCreatedAt(DateUtil.getCurrentDate());
            update.setUpdatedAt(DateUtil.getCurrentDate());
            update.setId(request.getId());
            log.info("updateResourceReference() update: {}", update.toString());
            int updateFlag = resourceReferenceService.update(update);
            if(0 < updateFlag) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.SUCCESS, "update success", GrpcUtil.getResourceReference(update), response);
            } else {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INTERNAL_ERROR, "update error", null, response);
            }
        }  catch (Exception ex) {
            log.error("updateResourceReference() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.resourceReferenceResponse(CodeProto.Code.INTERNAL_ERROR, "update error", null, response);
        }
    }

    @Override
    public void deleteResourceReferenceById(ResourceReferenceProto.DeleteResourceReferenceByIdRequest request,
                                             StreamObserver<ResourceReferenceProto.ResourceReferenceCommonResponse> response) {
        try {
            log.info("deleteResourceReferenceById() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getId() < 0) {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "deleteResourceReferenceById param error", response);
                return;
            }
            int deleteFlag = resourceReferenceService.deleteById(request.getId());
            if (0 < deleteFlag) {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.SUCCESS, "delete success", response);
            } else {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
            }
        } catch (Exception ex) {
            log.error("deleteResourceReferenceById() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
        }
    }

    @Override
    public void deleteResourceReferenceByIds(ResourceReferenceProto.DeleteResourceReferenceByIdsRequest request,
                                             StreamObserver<ResourceReferenceProto.ResourceReferenceCommonResponse> response) {
        try {
            log.info("deleteResourceReferenceByIds() grpc service receive: {}", request != null ? request.toString() : "");
            if (CollectionUtils.isEmpty(request.getIdList())) {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "deleteResourceReferenceByIds param error", response);
                return;
            }
            int deleteFlag = resourceReferenceService.deleteByIds(request.getIdList());
            if (0 < deleteFlag) {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.SUCCESS, "delete success", response);
            } else {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
            }
        } catch (Exception ex) {
            log.error("deleteResourceReferenceByIds() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
        }
    }

    @Override
    public void deleteSelfByTypeAndId(ResourceReferenceProto.DeleteByTypeAndIdRequest request,
                                             StreamObserver<ResourceReferenceProto.ResourceReferenceCommonResponse> response) {
        try {
            log.info("deleteSelfByTypeAndId() grpc service receive: {}", request != null ? request.toString() : "");
            if (StringUtils.isEmpty(request.getType()) || request.getId() <= 0) {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "deleteSelfByTypeAndId param error", response);
                return;
            }
            int deleteFlag = resourceReferenceService.deleteSelfByTypeAndId(request.getType(), request.getId());
            if (0 < deleteFlag) {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.SUCCESS, "delete success", response);
            } else {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
            }
        } catch (Exception ex) {
            log.error("deleteSelfByTypeAndId() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
        }
    }

    @Override
    public void deleteTargetByTypeAndId(ResourceReferenceProto.DeleteByTypeAndIdRequest request,
                                      StreamObserver<ResourceReferenceProto.ResourceReferenceCommonResponse> response) {
        try {
            log.info("deleteTargetByTypeAndId() grpc service receive: {}", request != null ? request.toString() : "");
            if (StringUtils.isEmpty(request.getType()) || request.getId() <= 0) {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "deleteTargetByTypeAndId param error", response);
                return;
            }
            int deleteFlag = resourceReferenceService.deleteTargetByTypeAndId(request.getType(), request.getId());
            if (0 < deleteFlag) {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.SUCCESS, "delete success", response);
            } else {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
            }
        } catch (Exception ex) {
            log.error("deleteTargetByTypeAndId() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
        }
    }

    @Override
    public void deleteByTypeAndId(ResourceReferenceProto.DeleteByTypeAndIdRequest request,
                                        StreamObserver<ResourceReferenceProto.ResourceReferenceCommonResponse> response) {
        try {
            log.info("deleteByTypeAndId() grpc service receive: {}", request != null ? request.toString() : "");
            if (StringUtils.isEmpty(request.getType()) || request.getId() <= 0) {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "deleteByTypeAndId param error", response);
                return;
            }
            int deleteFlag = resourceReferenceService.deleteByTypeAndId(request.getType(), request.getId());
            if (0 < deleteFlag) {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.SUCCESS, "delete success", response);
            } else {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
            }
        } catch (Exception ex) {
            log.error("deleteByTypeAndId() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
        }
    }

    @Override
    public void deleteByProjectId(ResourceReferenceProto.DeleteByProjectIdRequest request,
                                  StreamObserver<ResourceReferenceProto.ResourceReferenceCommonResponse> response) {
        try {
            log.info("deleteByProjectId() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getProjectId() <= 0) {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INVALID_PARAMETER, "deleteByProjectId param error", response);
                return;
            }
            int deleteFlag = resourceReferenceService.deleteByProjectId(request.getProjectId());
            if (0 < deleteFlag) {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.SUCCESS, "delete success", response);
            } else {
                GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
            }
        } catch (Exception ex) {
            log.error("deleteByProjectId() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.resourceReferenceCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
        }
    }

    @Override
    public void countByTarget(ResourceReferenceProto.CountByTargetRequest request,
                         StreamObserver<ResourceReferenceProto.CountResourceReferenceResponse> response) {
        try {
            log.info("countByTarget() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getTargetProjectId() <= 0 || request.getTargetIid() <= 0) {
                GrpcUtil.countByTargetResponse(CodeProto.Code.INVALID_PARAMETER, "countByTarget param error", 0, response);
                return;
            }
            int counts = resourceReferenceService.countByTarget(request.getTargetProjectId(), request.getTargetIid());
            GrpcUtil.countByTargetResponse(CodeProto.Code.SUCCESS, "count success", counts, response);
        } catch (Exception ex) {
            log.error("countByTarget() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.countByTargetResponse(CodeProto.Code.INTERNAL_ERROR, "count error", 0, response);
        }
    }

    @Override
    public void countBySelfWithTargetDeleted(ResourceReferenceProto.CountBySelfWithTargetDeletedRequest request,
                                             StreamObserver<ResourceReferenceProto.CountResourceReferenceResponse> response) {
        try {
            log.info("countBySelfWithTargetDeleted() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getProjectId() <= 0 || request.getCode() <= 0) {
                GrpcUtil.countByTargetResponse(CodeProto.Code.INVALID_PARAMETER, "countBySelfWithTargetDeleted param error", 0, response);
                return;
            }
            int counts = resourceReferenceService.countBySelfWithTargetDeleted(request.getProjectId(), request.getCode());
            GrpcUtil.countByTargetResponse(CodeProto.Code.SUCCESS, "count success", counts, response);
        } catch (Exception ex) {
            log.error("countBySelfWithTargetDeleted() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.countByTargetResponse(CodeProto.Code.INTERNAL_ERROR, "count error", 0, response);
        }
    }

    @Override
    public void findListByTargetType(ResourceReferenceProto.FindListByTargetTypeRequest request,
                                     StreamObserver<ResourceReferenceProto.FindResourceReferenceListResponse> response) {
        try {
            log.info("findListByTargetType() grpc service receive: {}", request != null ? request.toString() : "");
            if (StringUtils.isEmpty(request.getTargetType()) || request.getTargetId() <= 0) {
                GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INVALID_PARAMETER, "findListByTargetType param error", null, response);
                return;
            }
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceService.findListByTargetType(request.getTargetType(), request.getTargetId()),
                    response);
        } catch (Exception ex) {
            log.error("findListByTargetType() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void findListByTargetProjectId(ResourceReferenceProto.FindListByTargetProjectIdRequest request,
                                     StreamObserver<ResourceReferenceProto.FindResourceReferenceListResponse> response) {
        try {
            log.info("findListByTargetProjectId() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getTargetProjectId() <= 0 || request.getTargetIid() <= 0) {
                GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INVALID_PARAMETER, "findListByTargetProjectId param error", null, response);
                return;
            }
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceService.findListByTargetProjectId(request.getTargetProjectId(), request.getTargetIid(), request.getUserId(), request.getIsFilter()),
                    response);
        } catch (Exception ex) {
            log.error("findListByTargetProjectId() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void findListBySelfType(ResourceReferenceProto.FindListBySelfTypeRequest request,
                                     StreamObserver<ResourceReferenceProto.FindResourceReferenceListResponse> response) {
        try {
            log.info("findListBySelfType() grpc service receive: {}", request != null ? request.toString() : "");
            if (StringUtils.isEmpty(request.getSelfType()) || request.getSelfId() <= 0) {
                GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INVALID_PARAMETER, "findListBySelfType param error", null, response);
                return;
            }
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceService.findListBySelfType(request.getSelfType(), request.getSelfId()),
                    response);
        } catch (Exception ex) {
            log.error("findListBySelfType() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void findListBySelfProjectId(ResourceReferenceProto.FindListBySelfProjectIdRequest request,
                                   StreamObserver<ResourceReferenceProto.FindResourceReferenceListResponse> response) {
        try {
            log.info("findListBySelfProjectId() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getSelfProjectId() <= 0 || request.getSelfIid() <= 0) {
                GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INVALID_PARAMETER, "findListBySelfProjectId param error", null, response);
                return;
            }
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceService.findListBySelfProjectId(request.getSelfProjectId(), request.getSelfIid()),
                    response);
        } catch (Exception ex) {
            log.error("findListBySelfProjectId() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void findListBySelfAndTarget(ResourceReferenceProto.FindlistBySelfAndTargetRequest request,
                                        StreamObserver<ResourceReferenceProto.FindResourceReferenceListResponse> response) {
        try {
            log.info("findListBySelfAndTarget() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getProjectId() <= 0 || request.getSelfAndTargetId() <= 0) {
                GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INVALID_PARAMETER, "findListBySelfAndTarget param error", null, response);
                return;
            }
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceService.findListBySelfAndTarget(request.getProjectId(), request.getSelfAndTargetId()),
                    response);
        } catch (Exception ex) {
            log.error("findListBySelfAndTarget() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void findReferMutuallyList(ResourceReferenceProto.FindReferMutuallyListRequest request,
                                   StreamObserver<ResourceReferenceProto.FindResourceReferenceListResponse> response) {
        try {
            log.info("findReferMutuallyList() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getSelfProjectId() <= 0 || request.getSelfIid() <= 0 ) {
                GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INVALID_PARAMETER, "findReferMutuallyList param error", null, response);
                return;
            }
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceService.findReferMutuallyList(request.getSelfProjectId(), request.getSelfIid(), request.getUserId(), request.getIsFilter()),
                    response);
        } catch (Exception ex) {
            log.error("findReferMutuallyList() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void findMutuallyList(ResourceReferenceProto.FindMutuallyListRequest request,
                                   StreamObserver<ResourceReferenceProto.FindResourceReferenceListResponse> response) {
        try {
            log.info("findMutuallyList() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getSelfProjectId() <= 0 || request.getSelfCode() <= 0 || request.getTargetProjectId() <= 0 || request.getTargetCode() <= 0) {
                GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INVALID_PARAMETER, "findMutuallyList param error", null, response);
                return;
            }
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceService.findMutuallyList(request.getSelfProjectId(), request.getSelfCode(), request.getTargetProjectId(), request.getTargetCode()),
                    response);
        } catch (Exception ex) {
            log.error("findMutuallyList() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void findIdsMutually(ResourceReferenceProto.FindIdsMutuallyRequest request,
                                 StreamObserver<ResourceReferenceProto.FindIdsMutuallyResponse> response) {
        try {
            log.info("findIdsMutually() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getProjectId() <= 0 || request.getCode() <= 0) {
                GrpcUtil.findIdsMutuallyResponse(CodeProto.Code.INVALID_PARAMETER, "findIdsMutually param error", null, response);
                return;
            }
            GrpcUtil.findIdsMutuallyResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceService.findIdsMutually(request.getProjectId(), request.getCode()),
                    response);
        } catch (Exception ex) {
            log.error("findIdsMutually() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.findIdsMutuallyResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void findBySelfWithDescriptionCitedRelation(ResourceReferenceProto.FindBySelfWithDescriptionCitedRelationRequest request,
                                StreamObserver<ResourceReferenceProto.FindResourceReferenceListResponse> response) {
        try {
            log.info("findBySelfWithDescriptionCitedRelation() grpc service receive: {}", request != null ? request.toString() : "");
            if (StringUtils.isEmpty(request.getSelfType()) || request.getSelfId() <= 0) {
                GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INVALID_PARAMETER, "findBySelfWithDescriptionCitedRelation param error", null, response);
                return;
            }
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceService.findBySelfWithDescriptionCitedRelation(request.getSelfType(), request.getSelfId()),
                    response);
        } catch (Exception ex) {
            log.error("findBySelfWithDescriptionCitedRelation() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void findBySelfWithoutDescriptionCitedRelation(ResourceReferenceProto.FindBySelfWithoutDescriptionCitedRelationRequest request,
                                StreamObserver<ResourceReferenceProto.FindResourceReferenceListResponse> response) {
        try {
            log.info("findBySelfWithoutDescriptionCitedRelation() grpc service receive: {}", request != null ? request.toString() : "");
            if (StringUtils.isEmpty(request.getSelfType()) || request.getSelfId() <= 0) {
                GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INVALID_PARAMETER, "findBySelfWithoutDescriptionCitedRelation param error", null, response);
                return;
            }
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceService.findBySelfWithoutDescriptionCitedRelation(request.getSelfType(), request.getSelfId()),
                    response);
        } catch (Exception ex) {
            log.error("findBySelfWithoutDescriptionCitedRelation() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void findBySelfWithTargetDeleted(ResourceReferenceProto.FindBySelfWithTargetDeletedRequest request,
                                StreamObserver<ResourceReferenceProto.FindResourceReferenceListResponse> response) {
        try {
            log.info("findBySelfWithTargetDeleted() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getProjectId() <= 0 || request.getCode() <= 0) {
                GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INVALID_PARAMETER, "findBySelfWithTargetDeleted param error", null, response);
                return;
            }
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceService.findBySelfWithTargetDeleted(request.getProjectId(), request.getCode(), request.getUserId(), request.getIsFilter()),
                    response);
        } catch (Exception ex) {
            log.error("findBySelfWithTargetDeleted() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void findByProjectId(ResourceReferenceProto.FindByProjectIdRequest request,
                                           StreamObserver<ResourceReferenceProto.FindResourceReferenceListResponse> response) {
        try {
            log.info("findByProjectId() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getProjectId() <= 0) {
                GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INVALID_PARAMETER, "findByProjectId param error", null, response);
                return;
            }
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceService.findByProjectId(request.getProjectId(), request.getWithDeleted()),
                    response);
        } catch (Exception ex) {
            log.error("findByProjectId() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void getByProjectIdAndCode(ResourceReferenceProto.GetByProjectIdAndCodeRequest request,
                                      StreamObserver<ResourceReferenceProto.ResourceReferenceResponse> response) {
        try {
            log.info("getByProjectIdAndCode() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getSelfProjectId() <= 0 || request.getSelfCode() <= 0 || request.getTargetProjectId() <= 0 || request.getTargetCode() <= 0) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INVALID_PARAMETER, "getByProjectIdAndCode param error", null, response);
            }
            ResourceReference resourceReference = resourceReferenceService.getByProjectIdAndCode(request.getSelfProjectId(),
                    request.getSelfCode(), request.getTargetProjectId(), request.getTargetCode());
            if(Objects.nonNull(resourceReference)) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.SUCCESS,
                        "get success",
                        GrpcUtil.getResourceReference(resourceReference),
                        response
                );
            } else {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INVALID_PARAMETER, "result is empty", null, response);
            }
        } catch (Exception ex) {
            log.error("getByProjectIdAndCode() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.resourceReferenceResponse(CodeProto.Code.INTERNAL_ERROR, "get error", null, response);
        }
    }

    @Override
    public void getByTypeAndId(ResourceReferenceProto.GetByTypeAndIdRequest request,
                                      StreamObserver<ResourceReferenceProto.ResourceReferenceResponse> response) {
        try {
            log.info("getByTypeAndId() grpc service receive: {}", request != null ? request.toString() : "");
            if (StringUtils.isEmpty(request.getSelfType()) || request.getSelfId() <= 0 || StringUtils.isEmpty(request.getTargetType()) || request.getTargetId() <= 0) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INVALID_PARAMETER, "getByTypeAndId param error", null, response);
            }
            ResourceReference resourceReference = resourceReferenceService.getByTypeAndId(request.getSelfType(),
                    request.getSelfId(), request.getTargetType(), request.getTargetId());
            if(Objects.nonNull(resourceReference)) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.SUCCESS,
                        "get success",
                        GrpcUtil.getResourceReference(resourceReference),
                        response
                );
            } else {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INVALID_PARAMETER, "result is empty", null, response);
            }
        } catch (Exception ex) {
            log.error("getByTypeAndId() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.resourceReferenceResponse(CodeProto.Code.INTERNAL_ERROR, "get error", null, response);
        }
    }

    @Override
    public void getOptional(ResourceReferenceProto.GetOptionalRequest request,
                                      StreamObserver<ResourceReferenceProto.ResourceReferenceResponse> response) {
        try {
            log.info("getOptional() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getSelfProjectId() <= 0 || StringUtils.isEmpty(request.getSelfType()) ||
                    request.getSelfId() <= 0 || StringUtils.isEmpty(request.getTargetType())) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INVALID_PARAMETER, "getOptional param error", null, response);
            }
            ResourceReference resourceReference = resourceReferenceService.getOptional(request.getSelfProjectId(),
                    request.getSelfType(), request.getSelfId(), request.getTargetType());
            if(Objects.nonNull(resourceReference)) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.SUCCESS,
                        "get success",
                        GrpcUtil.getResourceReference(resourceReference),
                        response
                );
            } else {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INVALID_PARAMETER, "result is empty", null, response);
            }
        } catch (Exception ex) {
            log.error("getOptional() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.resourceReferenceResponse(CodeProto.Code.INTERNAL_ERROR, "get error", null, response);
        }
    }

    @Override
    public void getById(ResourceReferenceProto.GetByIdRequest request,
                                   StreamObserver<ResourceReferenceProto.ResourceReferenceResponse> response) {
        try {
            log.info("getWithDeletedById() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getId() <= 0) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INVALID_PARAMETER, "getWithDeletedById param error", null, response);
            }
            ResourceReference resourceReference = resourceReferenceService.getById(request.getId(), request.getWithDeleted());
            if(Objects.nonNull(resourceReference)) {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.SUCCESS,
                        "get success",
                        GrpcUtil.getResourceReference(resourceReference),
                        response
                );
            } else {
                GrpcUtil.resourceReferenceResponse(CodeProto.Code.INVALID_PARAMETER, "result is empty", null, response);
            }
        } catch (Exception ex) {
            log.error("getWithDeletedById() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.resourceReferenceResponse(CodeProto.Code.INTERNAL_ERROR, "get error", null, response);
        }
    }

    @Override
    public void findWithDeletedByIds(ResourceReferenceProto.FindWithDeletedByIdsRequest request,
                                     StreamObserver<ResourceReferenceProto.FindResourceReferenceListResponse> response) {
        try {
            log.info("findWithDeletedByIds() grpc service receive: {}", request != null ? request.toString() : "");
            if (CollectionUtils.isEmpty(request.getIdList())) {
                GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INVALID_PARAMETER, "getWithDeletedById param error", null, response);
            }
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceService.getWithDeletedByIds(request.getIdList()),
                    response);
        } catch (Exception ex) {
            log.error("findWithDeletedByIds() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.findResourceReferenceListResponse(CodeProto.Code.INTERNAL_ERROR, "get error", null, response);
        }
    }

    @Override
    public void existsResourceReference(ResourceReferenceProto.ExistsResourceReferenceRequest request,
                                        StreamObserver<ResourceReferenceProto.ExistsResourceReferenceResponse> response) {
        try {
            log.info("existsResourceReference() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getTargetProjectId() <= 0 || request.getTargetIid() <= 0 || request.getSelfProjectId() <= 0
                    || request.getTargetIid() <= 0) {
                GrpcUtil.existsResourceReferenceResponse(CodeProto.Code.INVALID_PARAMETER, "param error", false, response);
            }
            GrpcUtil.existsResourceReferenceResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceService.existsResourceReference(request.getTargetProjectId(), request.getTargetIid(),
                            request.getSelfProjectId(), request.getSelfIid()),
                    response);
        } catch (Exception ex) {
            log.error("existsResourceReference() callException request={}, ex={}", request != null ? request.toString() : "", ex);
            GrpcUtil.existsResourceReferenceResponse(CodeProto.Code.INTERNAL_ERROR, "exists error", false, response);
        }
    }
}