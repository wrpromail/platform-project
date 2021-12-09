package net.coding.lib.project.group;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface ProjectGroupProjectDao extends tk.mybatis.mapper.common.Mapper<ProjectGroupProject> {
    int batchDelete(List<Integer> ids);

    List<ProjectGroupProject> getByProjectId(
            @Param("projectId") int projectId,
            @Param("deletedAt") Timestamp defaultDeletedAt
    );

    List<ProjectGroupProject> getByProjectIdsAndUserId(
            @Param("projectIds") List<Integer> projectIds,
            @Param("userId") Integer userId,
            @Param("deletedAt") Timestamp defaultDeletedAt
    );

    void deleteGroupRelation(
            @Param("projectGroupId") Integer projectGroupId,
            @Param("userId") Integer userId
    );

    List<ProjectGroupProject> listByOwner(
            @Param("ownerId") Integer ownerId,
            @Param("groupId") Integer groupId,
            @Param("deleted") Timestamp deletedAt
    );

    List<ProjectGroupProject> listByGroup(
            @Param("projectGroupId") Integer projectGroupId,
            @Param("deletedAt") Timestamp defaultDeletedAt
    );

    int batchInsert(List<ProjectGroupProject> projectGroupProjectList);

    int batchUpdate(List<ProjectGroupProject> updateProjectGroupProjectList);
}
