package net.coding.lib.project.dao;

import net.coding.lib.project.entity.DeployTokens;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DeployTokensDao {
    DeployTokens selectByPrimaryKey(Integer id);

    List<DeployTokens> selectByDeployTokens(DeployTokens deployTokens);
}