package net.coding.lib.project.dao;

import net.coding.lib.project.entity.DeployTokenArtifacts;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface DeployTokenArtifactsDao {

    DeployTokenArtifacts selectByPrimaryKey(Integer id);

    List<DeployTokenArtifacts> getDeployTokenArtifacts(
            @Param("deployTokenId") Integer deployTokenId,
            @Param("deletedAt") Timestamp deletedAt);

    int deleteByDeployTokenArtifacts(
            @Param("deployTokenId") Integer deployTokenId,
            @Param("deletedAt") Timestamp deletedAt);

    int insert(DeployTokenArtifacts deployTokenArtifacts);
}