package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ProjectTokenDepot;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

/**
 * @Author liuying
 * @Date 2021/1/11 10:54 上午
 * @Version 1.0
 */
@Mapper
public interface ProjectTokenDepotDao {
    List<ProjectTokenDepot> getProjectTokenDepot(
            @Param("deployTokenId") Integer deployTokenId,
            @Param("deletedAt")
                    Timestamp deletedAt);

    int deleteByProjectTokenDepot(
            @Param("deployTokenId") Integer deployTokenId,
            @Param("deletedAt") Timestamp deletedAt);

    int insert(ProjectTokenDepot deployTokenDepot);

}