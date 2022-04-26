package net.coding.lib.project.dao;


import net.coding.lib.project.entity.ProjectMember;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface ProjectMemberInvisibleDao extends tk.mybatis.mapper.common.Mapper<ProjectMember> {

    List<ProjectMember> findPrincipalMembers(
            @Param("teamId") Integer teamId,
            @Param("principalType") String principalType,
            @Param("principalIds") Set<String> principalIds
    );

    List<ProjectMember> findJoinPrincipalMembers(
            @Param("teamId") Integer teamId,
            @Param("userId") Integer userId
    );
}