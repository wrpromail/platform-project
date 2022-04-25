package net.coding.app.project.utils;

import net.coding.common.base.util.QiniuCDNReplaceUtil;
import net.coding.common.constants.ProjectConstants;
import net.coding.common.util.BeanUtils;
import net.coding.common.util.ResultPage;
import net.coding.common.util.TextUtils;
import net.coding.lib.project.AppProperties;
import net.coding.lib.project.dao.ProgramProjectDao;
import net.coding.lib.project.entity.ProgramProject;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.utils.DateUtil;
import net.coding.proto.open.api.project.ProjectProto;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class ProtoConvertUtils {

    @Autowired
    private ProgramProjectDao programProjectDao;

    @Autowired
    private AppProperties appProperties;


    public ProjectProto.ProjectsData describeProjectPagesToProto(ResultPage<Project> resultPage) {
        List<ProjectProto.Project> projects = Optional.ofNullable(resultPage)
                .map(result -> describeProjectsToProtoList(result.getList()))
                .orElse(Collections.emptyList());
        Objects.requireNonNull(resultPage);
        return ProjectProto.ProjectsData.newBuilder()
                .setPageSize(resultPage.getPageSize())
                .setPageNumber(resultPage.getPage())
                .setTotalCount((int) resultPage.getTotalRow())
                .addAllProjectList(projects)
                .build();
    }

    /**
     * EnterpriseProjectProto
     * <p>
     * 项目信息列表转为Proto
     */
    public List<ProjectProto.Project> describeProjectsToProtoList(List<Project> projects) {
        return Optional.ofNullable(projects)
                .orElse(Collections.emptyList())
                .stream()
                .map(this::describeProjectToProto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * ProjectProto
     * <p>
     * 单个项目转为Proto
     */
    public ProjectProto.Project describeProjectToProto(Project project) {
        String icon = project.getIcon();
        if (StringUtils.startsWith(icon, "/")) {
            icon = appProperties.getDomain().getSchemaHome() + icon;
        }
        icon = QiniuCDNReplaceUtil.replace(icon);
        Set<Integer> programIds = programProjectDao.select(ProgramProject.builder()
                .projectId(project.getId())
                .deletedAt(BeanUtils.getDefaultDeletedAt())
                .build()
        )
                .stream()
                .map(ProgramProject::getProgramId)
                .collect(Collectors.toSet());
        return ProjectProto.Project.newBuilder()
                .setId(project.getId())
                .setCreatedAt(project.getCreatedAt().getTime())
                .setUpdatedAt(project.getUpdatedAt().getTime())
                .setStatus(Optional.ofNullable(project.getStatus()).orElse((short) 0))
                .setType(Optional.ofNullable(project.getType()).orElse(0))
                .setMaxMember(project.getMaxMember())
                .setName(project.getName())
                .setDisplayName(project.getDisplayName())
                .setDescription(TextUtils.filterUserInputContent(project.getDescription()))
                .setIcon(icon)
                .setUserOwnerId(ObjectUtils.defaultIfNull(project.getUserOwnerId(), -1))
                .setTeamOwnerId(ObjectUtils.defaultIfNull(project.getTeamOwnerId(), -1))
                .setStartDate(Optional.ofNullable(project.getStartDate()).map(DateUtil::DateToTime).orElse(0L))
                .setEndDate(Optional.ofNullable(project.getEndDate()).map(DateUtil::DateToTime).orElse(0L))
                .setTeamId(ObjectUtils.defaultIfNull(project.getTeamOwnerId(), -1))
                .setIsDemo(false)
                .setArchived(ProjectConstants.isArchived(DateUtil.dateToStr(project.getDeletedAt())))
                .addAllProgramIds(programIds)
                .build();
    }

}
