package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ProjectMember;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface ProjectMemberDao {

    int insert(ProjectMember record);

    int update(ProjectMember record);

    ProjectMember getById(@Param("id") Integer id);

    List<ProjectMember> findListByProjectId(@Param("projectId") Integer projectId,
                                            @Param("deletedAt") Timestamp deletedAt);

    ProjectMember getByProjectIdAndUserId(@Param("projectId") Integer projectId,
                                          @Param("userId") Integer userId,
                                          @Param("deletedAt") Timestamp deletedAt);
}
