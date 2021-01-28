package net.coding.lib.project.dao;

import net.coding.lib.project.entity.DeployTokenDepot;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author liuying
 * @Date 2021/1/11 10:54 上午
 * @Version 1.0
 */
@Mapper
public interface DeployTokenDepotsDao {
    List<DeployTokenDepot> getDeployTokenDepot(@Param("deployTokenId") Integer deployTokenId);

    int deleteByDeployTokenDepot(@Param("deployTokenId") Integer deployTokenId);

    int insert(DeployTokenDepot deployTokenDepot);

}