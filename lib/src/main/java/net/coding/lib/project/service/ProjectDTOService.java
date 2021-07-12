package net.coding.lib.project.service;

import net.coding.common.util.BeanUtils;
import net.coding.common.util.TextUtils;
import net.coding.lib.project.dto.ProjectDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.grpc.client.TeamGrpcClient;
import net.coding.lib.project.utils.DateUtil;

import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import proto.platform.team.TeamProto;

@Slf4j
@Service
@AllArgsConstructor
public class ProjectDTOService {
    private final TeamGrpcClient teamGrpcClient;

    public ProjectDTO toListDTO(Project project) {
        if (project == null) {
            return null;
        }
        return ProjectDTO.builder()
                .id(Optional.ofNullable(project.getId()).orElse(0))
                .description(TextUtils.htmlEscape(project.getDescription()))
                .name(project.getName())
                .display_name(project.getDisplayName())
                .start_date(DateUtil.formatDateToStr(project.getStartDate()))
                .end_date(DateUtil.formatDateToStr(project.getEndDate()))
                .icon(project.getIcon())
                .pmType(project.getPmType())
                .build();
    }

    public ProjectDTO toDetailDTO(Project project) {
        if (project == null) {
            return null;
        }
        return ProjectDTO.builder()
                .id(Optional.ofNullable(project.getId()).orElse(0))
                .description(TextUtils.htmlEscape(project.getDescription()))
                .name(project.getName())
                .display_name(project.getDisplayName())
                .start_date(DateUtil.formatDateToStr(project.getStartDate()))
                .end_date(DateUtil.formatDateToStr(project.getEndDate()))
                .icon(project.getIcon())
                .pmType(project.getPmType())
                .project_path("/p/" + project.getName())
                .created_at(Timestamp.from(project.getCreatedAt().toInstant()))
                .deleted_at(Timestamp.from(project.getDeletedAt().toInstant()))
                .is_member(true)
                .isDemo(false)
                .archived(DateUtil.strToDate(BeanUtils.ARCHIVED_AT).equals(project.getDeletedAt()))
                .invisible(project.getInvisible())
                .name_pinyin(project.getNamePinyin())
                .owner_user_name(getTeamDomain(project.getTeamOwnerId()))
                .build();
    }

    private String getTeamDomain(Integer teamId) {
        TeamProto.GetTeamResponse response = teamGrpcClient.getTeam(teamId);
        return Optional.ofNullable(response)
                .map(TeamProto.GetTeamResponse::getData)
                .map(TeamProto.Team::getGlobalKey)
                .orElse(null);
    }
}
