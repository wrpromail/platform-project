package net.coding.lib.project.hook.trigger;import net.coding.lib.project.entity.Project;import net.coding.lib.project.entity.ProjectMember;import net.coding.lib.project.grpc.client.UserGrpcClient;import net.coding.service.hook.definition.event.ServiceHookEvent;import net.coding.service.hook.definition.message.ServiceHookMessage;import org.springframework.stereotype.Component;import java.util.List;import lombok.extern.slf4j.Slf4j;@Component@Slf4jpublic class UpdateMemberRoleEventTriggerTrigger extends AbstractMemberEventTrigger implements MemberEventTrigger<ProjectMember> {    private final ServiceHookMessage projectServiceHookMessage;    public UpdateMemberRoleEventTriggerTrigger(final UserGrpcClient userGrpcClient, final ServiceHookMessage projectServiceHookMessage) {        super(userGrpcClient);        this.projectServiceHookMessage = projectServiceHookMessage;    }    @Override    public void trigger(List<String> roleId, ProjectMember o, Project projectId, int currentUserId) {        projectServiceHookMessage.sendMessage(                projectId.getId(),                currentUserId,                ServiceHookEvent.MEMBER_ROLE_UPDATED,                body(o),                getCondition(roleId)        );    }}