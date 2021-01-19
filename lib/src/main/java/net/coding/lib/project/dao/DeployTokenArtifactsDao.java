package net.coding.lib.project.dao;

import net.coding.lib.project.entity.DeployTokenArtifacts;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DeployTokenArtifactsDao {

    DeployTokenArtifacts selectByPrimaryKey(Integer id);

    List<DeployTokenArtifacts> getDeployTokenArtifacts(DeployTokenArtifacts deployTokenArtifacts);
}