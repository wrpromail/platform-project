package net.coding.lib.project.utils;

import net.coding.common.util.TextUtils;
import net.coding.lib.project.dto.RoleDTO;
import net.coding.lib.project.dto.UserDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import proto.acl.AclProto;
import proto.platform.user.UserProto;

public class UserUtil {

    public static UserDTO toBuilderUser(UserProto.User user, boolean showSensitive) {
        UserDTO.UserDTOBuilder builder = UserDTO.builder();
        builder.teamId(user.getTeamId());
        builder.location(TextUtils.htmlEscape(user.getLocation()));
        builder.company(TextUtils.htmlEscape(user.getCompany()));
        builder.slogan(TextUtils.htmlEscape(user.getSlogan()));
        builder.introduction(TextUtil.htmlEscape(user.getIntroduction()));
        builder.avatar(user.getAvatar());
        builder.last_logined_at(user.getLastLoginedAt());
        builder.global_key(TextUtil.htmlEscape(user.getGlobalKey()));
        builder.name(TextUtil.htmlEscape(user.getName()));
        builder.name_pinyin(user.getNamePinyin());
        builder.status(Optional.ofNullable(user.getStatus()).orElse(1));
        builder.id(Optional.ofNullable(user.getId()).orElse(0));
        builder.email_validation(user.getEmailValidation());
        builder.phone_validation(user.getPhoneValidation());
        builder.account_type(user.getAccountType());
        if (showSensitive) {
            builder.email(user.getEmail());
            builder.phone(user.getPhone());
        }
        return builder.build();
    }

    public static List<RoleDTO> toRoleDTO(List<AclProto.Role> roles) {
        List<RoleDTO> list = new ArrayList<>();
        roles.forEach(role -> list.add(RoleDTO.builder()
                .name(role.getName())
                .roleType(role.getType())
                .roleId(role.getId()).build()));
        return list;

    }
}
