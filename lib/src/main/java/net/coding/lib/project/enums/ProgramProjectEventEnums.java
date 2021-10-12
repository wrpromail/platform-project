package net.coding.lib.project.enums;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;
import proto.platform.permission.PermissionProto;

import static net.coding.common.constants.ProjectConstants.ACTION_ARCHIVE;
import static net.coding.common.constants.ProjectConstants.ACTION_CREATE;
import static net.coding.common.constants.ProjectConstants.ACTION_DELETE;
import static net.coding.common.constants.ProjectConstants.ACTION_UNARCHIVE;
import static net.coding.common.constants.ProjectConstants.ACTION_UPDATE;
import static net.coding.lib.project.enums.PmTypeEnums.PROGRAM;
import static net.coding.lib.project.enums.PmTypeEnums.PROJECT;
import static net.coding.lib.project.enums.ProgramProjectEventEnums.ACTION.ACTION_VIEW;
import static org.apache.logging.log4j.util.Strings.EMPTY;
import static proto.platform.permission.PermissionProto.Action.Create;
import static proto.platform.permission.PermissionProto.Action.Delete;
import static proto.platform.permission.PermissionProto.Action.Update;
import static proto.platform.permission.PermissionProto.Action.View;
import static proto.platform.permission.PermissionProto.Function.EnterpriseProgram;
import static proto.platform.permission.PermissionProto.Function.EnterpriseProgramArchive;
import static proto.platform.permission.PermissionProto.Function.EnterpriseProject;
import static proto.platform.permission.PermissionProto.Function.EnterpriseProjectArchive;

@Getter
@AllArgsConstructor
public enum ProgramProjectEventEnums {

    viewProject(ACTION_VIEW, PROJECT.getType(), EMPTY, EnterpriseProject, View),
    createProject(ACTION_CREATE, PROJECT.getType(), "project_created", EnterpriseProject, Create),
    deleteProject(ACTION_DELETE, PROJECT.getType(), "project_deleted", EnterpriseProject, Delete),
    updateProject(ACTION_UPDATE, PROJECT.getType(), EMPTY, EnterpriseProject, Update),
    archiveProject(ACTION_ARCHIVE, PROJECT.getType(), "project_archive", EnterpriseProjectArchive, Create),
    unarchiveProject(ACTION_UNARCHIVE, PROJECT.getType(), "project_unarchive", EnterpriseProjectArchive, Delete),

    viewProgram(ACTION_VIEW, PROGRAM.getType(), EMPTY, EnterpriseProgram, View),
    createProgram(ACTION_CREATE, PROGRAM.getType(), "program_created", EnterpriseProgram, Create),
    deleteProgram(ACTION_DELETE, PROGRAM.getType(), "program_deleted", EnterpriseProgram, Delete),
    updateProgram(ACTION_UPDATE, PROGRAM.getType(), EMPTY, EnterpriseProgram, Update),
    archiveProgram(ACTION_ARCHIVE, PROGRAM.getType(), "program_archive", EnterpriseProgramArchive, Create),
    unarchiveProgram(ACTION_UNARCHIVE, PROGRAM.getType(), "program_unarchive", EnterpriseProgramArchive, Delete);


    private Short action;
    private Integer program;
    private String message;
    private PermissionProto.Function permissionFunction;
    private PermissionProto.Action permissionAction;

    public static ProgramProjectEventEnums of(Short action, Integer program) {
        return Arrays.stream(values())
                .filter(value -> value.action.equals(action) && value.program.equals(program))
                .findFirst()
                .orElse(null);
    }

    public static class ACTION {
        public static final Short ACTION_VIEW = 99;
    }
}
