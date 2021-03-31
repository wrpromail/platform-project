package net.coding.lib.project.hook.trigger;

import net.coding.lib.project.entity.ProjectMember;
import net.coding.lib.project.grpc.client.UserGrpcClient;
import net.coding.lib.project.hook.EventBody;
import net.coding.lib.project.hook.filter.MemberFilterProperty;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.user.UserProto;

@Slf4j
@AllArgsConstructor
public abstract class AbstractMemberEventTrigger {
    private final UserGrpcClient userGrpcClient;

    protected EventBody body(ProjectMember projectMember) {
        UserProto.User user = userGrpcClient.getUserById(projectMember.getUserId());
        return EventBody.builder()
                .member(
                        EventBody.User.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .name_pinyin(user.getNamePinyin())
                                .avatar_url(user.getAvatar())
                                .html_url(user.getHtmlUrl())
                                .url(user.getUrl())
                                .login(user.getGlobalKey())
                                .build()
                )
                .build();
    }


    protected Function<Map<String, String>, Boolean> getCondition(final List<String> roleId) {
        return p -> {
            Set<String> picked = StringUtils.commaDelimitedListToSet(p.get(MemberFilterProperty.MEMBER_ROLE_ID));
            if (CollectionUtils.isEmpty(picked)) {
                return true;
            }
            return CollectionUtils.containsAny(picked, roleId);
        };
    }
}
