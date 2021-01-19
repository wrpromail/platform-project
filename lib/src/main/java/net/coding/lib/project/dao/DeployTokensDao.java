package net.coding.lib.project.dao;

import net.coding.lib.project.entity.DeployTokens;
import net.coding.lib.project.parameter.DeployTokenUpdateParameter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeployTokensDao {
    DeployTokens selectByPrimaryKey(Integer id);

    List<DeployTokens> selectByDeployTokens(@Param("projectId") Integer projectId,
                                            @Param("type") Short type);

    int deleteDeployTokens(@Param("id") Integer id);

    int updateEnableDeployToken(@Param("id") Integer id, @Param("enabled") boolean enabled);

    int update(DeployTokenUpdateParameter parameter);

    int insert(DeployTokens deployTokens);
}