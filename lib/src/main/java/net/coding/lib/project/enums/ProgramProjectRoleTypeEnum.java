package net.coding.lib.project.enums;

import net.coding.platform.permission.proto.CommonProto;

import java.util.Arrays;

public class ProgramProjectRoleTypeEnum {

    public enum ProgramRoleTypeEnum {
        // 项目集负责人
        ProgramOwner((short) 100, CommonProto.RoleTypeEnum.RoleType.ProgramOwner),
        // 项目集管理员
        ProgramAdmin((short) 90, CommonProto.RoleTypeEnum.RoleType.ProgramAdmin),
        // 项目集成员
        ProgramMember((short) 80, CommonProto.RoleTypeEnum.RoleType.ProgramMember),
        // 项目集-项目成员
        ProgramProjectMember((short) 75, CommonProto.RoleTypeEnum.RoleType.ProgramProjectMember);

        private Short code;
        private CommonProto.RoleTypeEnum.RoleType roleType;


        ProgramRoleTypeEnum(Short code, CommonProto.RoleTypeEnum.RoleType roleType) {
            this.code = code;
            this.roleType = roleType;
        }

        public Short getCode() {
            return this.code;
        }

        public CommonProto.RoleTypeEnum.RoleType getRoleType() {
            return this.roleType;
        }

        public static ProgramRoleTypeEnum of(Short code) {
            return Arrays.stream(values())
                    .filter(value -> value.code.equals(code))
                    .findFirst()
                    .orElse(null);
        }
    }


    public enum ProjectRoleTypeEnum {
        // 项目管理员
        ProjectAdmin((short) 90, CommonProto.RoleTypeEnum.RoleType.ProjectAdmin),
        // 项目成员
        ProjectMember((short) 80, CommonProto.RoleTypeEnum.RoleType.ProjectMember);

        private Short code;
        private CommonProto.RoleTypeEnum.RoleType roleType;

        public Short getCode() {
            return this.code;
        }

        public CommonProto.RoleTypeEnum.RoleType getRoleType() {
            return this.roleType;
        }

        ProjectRoleTypeEnum(Short code, CommonProto.RoleTypeEnum.RoleType roleType) {
            this.code = code;
            this.roleType = roleType;
        }

        public static ProjectRoleTypeEnum of(Short code) {
            return Arrays.stream(values())
                    .filter(value -> value.code.equals(code))
                    .findFirst()
                    .orElse(null);
        }
    }

}
