package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ProjectTokenArtifact;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface ProjectTokenArtifactDao {

    ProjectTokenArtifact selectByPrimaryKey(Integer id);

    List<ProjectTokenArtifact> getProjectTokenArtifacts(
            @Param("deployTokenId") Integer deployTokenId,
            @Param("deletedAt") Timestamp deletedAt);

    int deleteByProjectTokenArtifacts(
            @Param("deployTokenId") Integer deployTokenId,
            @Param("deletedAt") Timestamp deletedAt);

    int insert(ProjectTokenArtifact projectTokenArtifact);
}