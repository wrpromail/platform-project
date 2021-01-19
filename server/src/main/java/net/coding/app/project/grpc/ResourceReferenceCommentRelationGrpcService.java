package net.coding.app.project.grpc;

import net.coding.app.project.utils.GrpcUtil;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.entity.ResourceReference;
import net.coding.lib.project.entity.ResourceReferenceCommentRelation;
import net.coding.lib.project.service.ResourceReferenceCommentRelationService;
import net.coding.lib.project.utils.DateUtil;
import net.coding.proto.platform.project.ResourceReferenceCommentRelationProto;
import net.coding.proto.platform.project.ResourceReferenceCommentRelationServiceGrpc;

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
public class ResourceReferenceCommentRelationGrpcService extends
        ResourceReferenceCommentRelationServiceGrpc.ResourceReferenceCommentRelationServiceImplBase {

    @Autowired
    private ResourceReferenceCommentRelationService resourceReferenceCommentRelationService;

    @Override
    public void addCommentRelation(ResourceReferenceCommentRelationProto.AddCommentRelationRequest request,
                                   StreamObserver<ResourceReferenceCommentRelationProto.AddCommentRelationResponse> response) {
        try {
            log.info("addCommentRelation() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getProjectId() <= 0 || request.getCommentId() < 0 || request.getResourceReferenceId() <= 0
                    || StringUtils.isEmpty(request.getResourceType()) || StringUtils.isEmpty(request.getCitedSource())) {
                GrpcUtil.addCommentRelationResponse(CodeProto.Code.INVALID_PARAMETER, "addCommentRelation parameters error", null, response);
                return;
            }

            ResourceReferenceCommentRelation insert = new ResourceReferenceCommentRelation();
            insert.setProjectId(request.getProjectId());
            insert.setResourceReferenceId(request.getResourceReferenceId());
            insert.setResourceType(request.getResourceType());
            insert.setCitedSource(request.getCitedSource());
            insert.setCommentId(request.getCommentId());
            insert.setCreatedAt(DateUtil.getCurrentDate());
            insert.setUpdatedAt(DateUtil.getCurrentDate());
            insert.setDeletedAt(DateUtil.strToDate("1970-01-01 00:00:00"));
            log.info("addCommentRelation() insert: {}", insert.toString());
            resourceReferenceCommentRelationService.insert(insert);
            if (Objects.nonNull(insert.getId()) && 0 < insert.getId()) {
                GrpcUtil.addCommentRelationResponse(CodeProto.Code.SUCCESS, "add success", GrpcUtil.getCommentRelation(insert), response);
            } else {
                GrpcUtil.addCommentRelationResponse(CodeProto.Code.INTERNAL_ERROR, "add error", null, response);
            }
        } catch (Exception ex) {
            log.error("addCommentRelation() callException request={}, ex={}", request != null ? request.toString() : "", ex.getMessage());
            GrpcUtil.addCommentRelationResponse(CodeProto.Code.INTERNAL_ERROR, "add error", null, response);
        }
    }

    @Override
    public void batchAddCommentRelation(ResourceReferenceCommentRelationProto.BatchAddCommentRelationRequest request,
                                        StreamObserver<ResourceReferenceCommentRelationProto.CommentRelationCommonResponse> response) {
        try {
            log.info("batchAddCommentRelation() grpc service receive: {}", request != null ? request.toString() : "");
            if (CollectionUtils.isEmpty(request.getAddCommentRelationRequestList())) {
                GrpcUtil.commentRelationCommonResponse(CodeProto.Code.INVALID_PARAMETER, "batchAddCommentRelation param error", response);
                return;
            }
            List<ResourceReferenceCommentRelation> list = new ArrayList<>();
            request.getAddCommentRelationRequestList().forEach(record -> {
                ResourceReferenceCommentRelation insert = new ResourceReferenceCommentRelation();
                insert.setProjectId(record.getProjectId());
                insert.setResourceReferenceId(record.getResourceReferenceId());
                insert.setResourceType(record.getResourceType());
                insert.setCitedSource(record.getCitedSource());
                insert.setCommentId(record.getCommentId());
                insert.setCreatedAt(DateUtil.getCurrentDate());
                insert.setUpdatedAt(DateUtil.getCurrentDate());
                insert.setDeletedAt(DateUtil.strToDate("1970-01-01 00:00:00"));
                list.add(insert);
            });
            int batchInsertFlag = resourceReferenceCommentRelationService.batchInsert(list);
            if (0 < batchInsertFlag) {
                GrpcUtil.commentRelationCommonResponse(CodeProto.Code.SUCCESS, "batchAdd success", response);
            } else {
                GrpcUtil.commentRelationCommonResponse(CodeProto.Code.INTERNAL_ERROR, "batchAdd error", response);
            }
        } catch (Exception ex) {
            log.error("commentRelationCommonResponse() callException request={}, ex={}", request != null ? request.toString() : "", ex.getMessage());
            GrpcUtil.commentRelationCommonResponse(CodeProto.Code.INTERNAL_ERROR, "batchAdd error", response);
        }
    }

    @Override
    public void deleteByReferenceIds(ResourceReferenceCommentRelationProto.DeleteByReferenceIdsRequest request,
                                     StreamObserver<ResourceReferenceCommentRelationProto.CommentRelationCommonResponse> response) {
        try {
            log.info("deleteByReferenceIds() grpc service receive: {}", request != null ? request.toString() : "");
            if (CollectionUtils.isEmpty(request.getReferenceIdList())) {
                GrpcUtil.commentRelationCommonResponse(CodeProto.Code.INVALID_PARAMETER, "deleteByReferenceIds param error", response);
                return;
            }
            int deleteFlag = resourceReferenceCommentRelationService.deleteByReferenceIds(request.getReferenceIdList(), request.getIsDescription());
            if (0 < deleteFlag) {
                GrpcUtil.commentRelationCommonResponse(CodeProto.Code.SUCCESS, "delete success", response);
            } else {
                GrpcUtil.commentRelationCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
            }
        } catch (Exception ex) {
            log.error("deleteByReferenceIds() callException request={}, ex={}", request != null ? request.toString() : "", ex.getMessage());
            GrpcUtil.commentRelationCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
        }
    }

    @Override
    public void deleteByCommentIdAndReferenceIds(ResourceReferenceCommentRelationProto.DeleteByCommentIdAndReferenceIdsRequest request,
                                                 StreamObserver<ResourceReferenceCommentRelationProto.CommentRelationCommonResponse> response) {
        try {
            log.info("deleteByReferenceIds() grpc service receive: {}", request != null ? request.toString() : "");
            if (CollectionUtils.isEmpty(request.getReferenceIdList()) || request.getCommentId() < 0) {
                GrpcUtil.commentRelationCommonResponse(CodeProto.Code.INVALID_PARAMETER, "deleteByReferenceIds param error", response);
                return;
            }
            int deleteFlag = resourceReferenceCommentRelationService.deleteByCommentIdAndReferenceIds(request.getCommentId(), request.getReferenceIdList());
            if (0 < deleteFlag) {
                GrpcUtil.commentRelationCommonResponse(CodeProto.Code.SUCCESS, "delete success", response);
            } else {
                GrpcUtil.commentRelationCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
            }
        } catch (Exception ex) {
            log.error("deleteByReferenceIds() callException request={}, ex={}", request != null ? request.toString() : "", ex.getMessage());
            GrpcUtil.commentRelationCommonResponse(CodeProto.Code.INTERNAL_ERROR, "delete error", response);
        }
    }

    @Override
    public void findByResourceReferenceId(ResourceReferenceCommentRelationProto.FindByResourceReferenceIdRequest request,
                                          StreamObserver<ResourceReferenceCommentRelationProto.FindCommentRelationResponse> response) {
        try {
            log.info("findByResourceReferenceId() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getReferenceId() <= 0) {
                GrpcUtil.findCommentRelationListResponse(CodeProto.Code.INVALID_PARAMETER, "findByResourceReferenceId param error", null, response);
                return;
            }
            GrpcUtil.findCommentRelationListResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceCommentRelationService.findByResourceReferenceId(request.getReferenceId()),
                    response);
        } catch (Exception ex) {
            log.error("findByResourceReferenceId() callException request={}, ex={}", request != null ? request.toString() : "", ex.getMessage());
            GrpcUtil.findCommentRelationListResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void findReferenceRelationsBelowEqual(ResourceReferenceCommentRelationProto.FindReferenceIdsRequest request,
                                                 StreamObserver<ResourceReferenceCommentRelationProto.FindReferenceIdsResponse> response) {
        try {
            log.info("findReferenceRelationsBelowEqual() grpc service receive: {}", request != null ? request.toString() : "");
            if (CollectionUtils.isEmpty(request.getReferenceIdList()) || request.getNumber() == 0) {
                GrpcUtil.findReferenceIdsResponse(CodeProto.Code.INVALID_PARAMETER, "findReferenceRelationsBelowEqual param error", null, response);
                return;
            }
            GrpcUtil.findReferenceIdsResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceCommentRelationService.findReferenceRelationsBelowEqual(request.getReferenceIdList(), request.getNumber()),
                    response);
        } catch (Exception ex) {
            log.error("findReferenceRelationsBelowEqual() callException request={}, ex={}", request != null ? request.toString() : "", ex.getMessage());
            GrpcUtil.findReferenceIdsResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void findReferenceRelationsAbove(ResourceReferenceCommentRelationProto.FindReferenceIdsRequest request,
                                            StreamObserver<ResourceReferenceCommentRelationProto.FindReferenceIdsResponse> response) {
        try {
            log.info("findReferenceRelationsAbove() grpc service receive: {}", request != null ? request.toString() : "");
            if (CollectionUtils.isEmpty(request.getReferenceIdList()) || request.getNumber() == 0) {
                GrpcUtil.findReferenceIdsResponse(CodeProto.Code.INVALID_PARAMETER, "findReferenceRelationsAbove param error", null, response);
                return;
            }
            GrpcUtil.findReferenceIdsResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceCommentRelationService.findReferenceRelationsAbove(request.getReferenceIdList(), request.getNumber()),
                    response);
        } catch (Exception ex) {
            log.error("findReferenceRelationsAbove() callException request={}, ex={}", request != null ? request.toString() : "", ex.getMessage());
            GrpcUtil.findReferenceIdsResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void findUsedReferenceIdsWithoutDescription(ResourceReferenceCommentRelationProto.FindUsedReferenceIdsWithoutDescriptionRequest request,
                                                       StreamObserver<ResourceReferenceCommentRelationProto.FindReferenceIdsResponse> response) {
        try {
            log.info("findUsedReferenceIdsWithoutDescription() grpc service receive: {}", request != null ? request.toString() : "");
            if (CollectionUtils.isEmpty(request.getReferenceIdList())) {
                GrpcUtil.findReferenceIdsResponse(CodeProto.Code.INVALID_PARAMETER, "findUsedReferenceIdsWithoutDescription param error", null, response);
                return;
            }
            GrpcUtil.findReferenceIdsResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceCommentRelationService.findUsedReferenceIdsWithoutDescription(request.getReferenceIdList()),
                    response);
        } catch (Exception ex) {
            log.error("findUsedReferenceIdsWithoutDescription() callException request={}, ex={}", request != null ? request.toString() : "", ex.getMessage());
            GrpcUtil.findReferenceIdsResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }

    @Override
    public void hasComment(ResourceReferenceCommentRelationProto.HasCommentRequest request,
                           StreamObserver<ResourceReferenceCommentRelationProto.HasCommentResponse> response) {
        boolean isComment = false;
        try {
            log.info("hasComment() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getReferenceId() <= 0) {
                GrpcUtil.hasCommentResponse(CodeProto.Code.INVALID_PARAMETER, "hasComment param error", isComment, response);
                return;
            }
            int counts = resourceReferenceCommentRelationService.countComment(request.getReferenceId());
            if (counts > 0) {
                isComment = true;
            }
            GrpcUtil.hasCommentResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    isComment,
                    response);
        } catch (Exception ex) {
            log.error("hasComment() callException request={}, ex={}", request != null ? request.toString() : "", ex.getMessage());
            GrpcUtil.hasCommentResponse(CodeProto.Code.INTERNAL_ERROR, "find error", isComment, response);
        }
    }

    @Override
    public void findByCommentIdAndCommentType(ResourceReferenceCommentRelationProto.FindByCommentIdAndCommentTypeRequest request,
                                              StreamObserver<ResourceReferenceCommentRelationProto.FindCommentRelationResponse> response) {
        try {
            log.info("findByCommentIdAndCommentType() grpc service receive: {}", request != null ? request.toString() : "");
            if (request.getCommentId() < 0 || StringUtils.isEmpty(request.getResourceType())) {
                GrpcUtil.findCommentRelationListResponse(CodeProto.Code.INVALID_PARAMETER, "findByCommentIdAndCommentType param error", null, response);
                return;
            }
            GrpcUtil.findCommentRelationListResponse(CodeProto.Code.SUCCESS,
                    "find success",
                    resourceReferenceCommentRelationService.findByCommentIdAndCommentType(request.getCommentId(), request.getResourceType()),
                    response);
        } catch (Exception ex) {
            log.error("findByCommentIdAndCommentType() callException request={}, ex={}", request != null ? request.toString() : "", ex.getMessage());
            GrpcUtil.findCommentRelationListResponse(CodeProto.Code.INTERNAL_ERROR, "find error", null, response);
        }
    }
}
