package net.coding.app.project.utils;

import net.coding.common.base.util.QiniuCDNReplaceUtil;
import net.coding.common.constants.ProjectConstants;
import net.coding.common.util.TextUtils;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.service.download.CodingSettings;
import net.coding.lib.project.utils.DateUtil;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import proto.open.api.project.ProjectProto;

@Component
public class ProtoConvertUtils {

    @Autowired
    private CodingSettings codingSettings;

    /**
     * EnterpriseProjectProto
     * <p>
     * 项目信息列表转为Proto
     */
    public List<ProjectProto.DescribeProjectResponse> describeProjectsToProtoList(List<Project> projects) {
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
    public ProjectProto.DescribeProjectResponse describeProjectToProto(Project project) {
        String icon = project.getIcon();
        if (StringUtils.startsWith(icon, "/")) {
            icon = codingSettings.getApp().hostWithProtocol() + icon;
        }
        icon = QiniuCDNReplaceUtil.replace(icon);
        return ProjectProto.DescribeProjectResponse.newBuilder()
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
                .build();
    }

    public net.coding.proto.open.api.project.ProjectProto.Project describeProjectToProjectProto(Project project) {
        String icon = project.getIcon();
        if (StringUtils.startsWith(icon, "/")) {
            icon = codingSettings.getApp().hostWithProtocol() + icon;
        }
        icon = QiniuCDNReplaceUtil.replace(icon);
        return net.coding.proto.open.api.project.ProjectProto.Project.newBuilder()
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
                .build();
    }

}
