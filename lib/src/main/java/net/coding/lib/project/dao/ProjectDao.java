package net.coding.lib.project.dao;


import net.coding.lib.project.entity.Project;
import net.coding.lib.project.parameter.ProjectPageQueryParameter;
import net.coding.lib.project.parameter.ProjectQueryParameter;
import net.coding.lib.project.parameter.ProjectUpdateParameter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface ProjectDao extends tk.mybatis.mapper.common.Mapper<Project> {

    Project getProjectById(@Param("id") Integer id);

    Project getProjectByIdAndTeamId(@Param("id") Integer id,
                                    @Param("teamOwnerId") Integer teamOwnerId);

    Project getProjectNotDeleteByIdAndTeamId(@Param("id") Integer id,
                                             @Param("teamOwnerId") Integer teamOwnerId);

    Project getProjectArchiveByIdAndTeamId(@Param("id") Integer id,
                                           @Param("teamOwnerId") Integer teamOwnerId);

    Project getProjectByNameAndTeamId(@Param("name") String name,
                                      @Param("teamOwnerId") Integer teamOwnerId);

    Project getProjectByDisplayNameAndTeamId(@Param("displayName") String displayName,
                                             @Param("teamOwnerId") Integer teamOwnerId);

    List<Project> getProjectPages(ProjectPageQueryParameter parameter);

    List<Project> getUserProjects(ProjectQueryParameter parameter);

    List<Project> getProjects(ProjectQueryParameter parameter);

    Integer updateBasicInfo(ProjectUpdateParameter project);

    Integer updateIcon(@Param("id") Integer id, @Param("icon") String icon);

    List<Project> getProjectsByIds(
            @Param("list") List<Integer> id,
            @Param("deletedAt") Timestamp deletedAt,
            @Param("archivedAt") Timestamp archivedAt
    );

    List<Project> getByIds(
            @Param("list") List<Integer> id,
            @Param("deletedAt") Timestamp deletedAt
    );

    long countProjectsByFilter(
            @Param("teamId") Integer teamId,
            @Param("userId") Integer userId,
            @Param("deletedAt") Timestamp defaultDeletedAt
    );
    List<Project> findByUserProjects(
            @Param("teamId") Integer teamId,
            @Param("userId") Integer userId,
            @Param("keyword") String keyword,
            @Param("groupId") Integer groupId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize,
            @Param("deletedAt") Timestamp defaultDeletedAt
    );
}