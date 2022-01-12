package net.coding.lib.project.dao;

import com.github.pagehelper.PageRowBounds;

import net.coding.lib.project.dto.ProjectTeamMemberDTO;
import net.coding.lib.project.entity.ProjectMember;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

@Mapper
public interface ProjectMemberDao {

    int insert(ProjectMember record);

    int insertList(
            @Param("list") Set<Integer> userIds,
            @Param("record") ProjectMember record
    );

    int update(ProjectMember record);

    ProjectMember getById(@Param("id") Integer id);

    List<ProjectMember> findListByProjectId(
            @Param("projectId") Integer projectId,
            @Param("deletedAt") Timestamp deletedAt
    );

    ProjectMember getByProjectIdAndUserId(
            @Param("projectId") Integer projectId,
            @Param("userId") Integer userId,
            @Param("deletedAt") Timestamp deletedAt
    );

    List<ProjectMember> getProjectMembers(
            @Param("projectId") Integer projectId,
            @Param("keyWord") String keyWord,
            @Param("roleId") Integer roleId,
            @Param("page") PageRowBounds page
    );

    ProjectMember getProjectMemberByUserAndProject(
            @Param("userId") Integer userId,
            @Param("projectId") Integer projectId,
            @Param("deletedAt") Timestamp deletedAt
    );

    int updateProjectMemberType(
            @Param("projectId") Integer projectId,
            @Param("userId") Integer targetUserId,
            @Param("type") short type,
            @Param("deletedAt") Timestamp deletedAt
    );

    int deleteMember(
            @Param("projectId") Integer projectId,
            @Param("userId") Integer targetUserId,
            @Param("deletedAt") Timestamp deletedAt
    );

    int updateVisitTime(
            @Param("id") Integer id,
            @Param("deletedAt") Timestamp deletedAt
    );

    List<ProjectTeamMemberDTO> getMemberWithProjectAndTeam(
            @Param("projectId") Integer projectId,
            @Param("keyWord") String keyWord,
            @Param("page") PageRowBounds page
    );

    long countByProjectId(@Param("projectId") Integer projectId);
}
