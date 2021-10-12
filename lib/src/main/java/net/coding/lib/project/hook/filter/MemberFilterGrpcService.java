package net.coding.lib.project.hook.filter;import net.coding.common.i18n.utils.LocaleMessageSource;import net.coding.common.util.StringUtils;import net.coding.lib.project.dto.RoleDTO;import net.coding.lib.project.exception.CoreException;import net.coding.lib.project.service.ProjectMemberService;import net.coding.proto.service.hook.FilterDefinitionSupplierServiceGrpc;import net.coding.proto.service.hook.ServiceHookFilterDefinition;import net.coding.proto.service.hook.ServiceHookFilterDefinition.FilterDefinitionRequest;import org.lognet.springboot.grpc.GRpcService;import org.springframework.stereotype.Component;import java.util.List;import io.grpc.stub.StreamObserver;import lombok.AllArgsConstructor;import lombok.extern.slf4j.Slf4j;/** * @version 1.0 * @Description * @Author wangbo * @Date 2021/3/1 4:31 下午 */@Component@GRpcService@Slf4j@AllArgsConstructorpublic class MemberFilterGrpcService extends FilterDefinitionSupplierServiceGrpc.FilterDefinitionSupplierServiceImplBase {    private final ProjectMemberService projectMemberService;    private final LocaleMessageSource localeMessageSource;    public static final String USER_GROUP_KEY = "user_role";    @Override    public void getDefinitions(            FilterDefinitionRequest request,            StreamObserver<ServiceHookFilterDefinition.FilterDefinitionResponse> responseObserver    ) {        try {            ServiceHookFilterDefinition.FilterDefinitionResponse.Builder responseBuilder =                    ServiceHookFilterDefinition.FilterDefinitionResponse                            .newBuilder()                            .setCode(0);            responseBuilder.setData(getRoleFilter(request));        responseObserver.onNext(responseBuilder.build());        responseObserver.onCompleted();    } catch(    CoreException e)    {        log.error("MemberFilterGRpcService:{}", e.getMessage());    }}    public ServiceHookFilterDefinition.FilterDefinition getRoleFilter(FilterDefinitionRequest request) throws CoreException {        List<RoleDTO> list = projectMemberService.findMemberCountByProjectId(                Integer.parseInt(request.getTargetId())        );        ServiceHookFilterDefinition.Tree.Builder tree = ServiceHookFilterDefinition                .Tree                .newBuilder()                .setIsMulti(true)                .addOption(                        ServiceHookFilterDefinition                                .Option                                .newBuilder()                                .setValue(StringUtils.EMPTY)                                .setLabel(localeMessageSource.getMessage("all_member_role"))                                .build()                );        list.forEach(r -> {            tree.addOption(                    ServiceHookFilterDefinition                            .Option                            .newBuilder()                            .setLabel(r.getName())                            .setValue(String.valueOf(r.getRoleId()))                            .build()            );        });        return ServiceHookFilterDefinition.FilterDefinition.newBuilder()                .addProperty(                        ServiceHookFilterDefinition                                .PropertyDefinition                                .newBuilder()                                .setName(MemberFilterProperty.MEMBER_ROLE_ID)                                .setLabel(localeMessageSource.getMessage(USER_GROUP_KEY))                                .setRequired(false) //不填过滤条件，则等于选了所有过滤条件                                .setTree(tree)                                .build()                )                .build();    }}