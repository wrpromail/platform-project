package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ProjectToken;
import net.coding.lib.project.parameter.DeployTokenUpdateParameter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface ProjectTokenDao {
    ProjectToken selectByPrimaryKey(
            @Param("id") Integer id,
            @Param("deletedAt") Timestamp deletedAt
    );

    List<ProjectToken> selectByProjectToken(
            @Param("projectId") Integer projectId,
            @Param("type") Short type
    );

    int deleteProjectToken(@Param("id") Integer id);

    int updateEnableProjectToken(
            @Param("id") Integer id,
            @Param("enabled") boolean enabled
    );

    int update(DeployTokenUpdateParameter parameter);

    int insert(ProjectToken projectToken);

    ProjectToken selectProjectToken(
            @Param("projectId") Integer projectId,
            @Param("type") Short type,
            @Param("tokenName") String tokenName,
            @Param("deletedAt") Timestamp deletedAt
    );

    int updateExpired(@Param("id") Integer id,
                      @Param("expiredAt") Timestamp expiredAt,
                      @Param("deletedAt") Timestamp deletedAt);

    ProjectToken selectByToken(
            @Param("token") String token,
            @Param("deletedAt") Timestamp deletedAt
    );

    ProjectToken selectByTokenAndProjectId(
            @Param("token") String token,
            @Param("projectId") Integer projectId,
            @Param("deletedAt") Timestamp deletedAt
    );

    ProjectToken selectByTokenAndGkId(
            @Param("token") String token,
            @Param("gkId") Integer gkId,
            @Param("deletedAt") Timestamp deletedAt
    );

    int updateScopeById(
            @Param("id") Integer id,
            @Param("scope") String scope,
            @Param("deletedAt") Timestamp deletedAt
    );
}