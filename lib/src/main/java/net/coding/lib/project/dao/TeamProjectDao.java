package net.coding.lib.project.dao;

import net.coding.lib.project.entity.TeamProject;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;


@Mapper
public interface TeamProjectDao extends tk.mybatis.mapper.common.Mapper<TeamProject> {
    List<TeamProject> getContainArchivedProjects(@Param("teamId") Integer teamId,
                                                 @Param("deletedAt") Timestamp deletedAt,
                                                 @Param("archivedAt") Timestamp archivedAt);
}
