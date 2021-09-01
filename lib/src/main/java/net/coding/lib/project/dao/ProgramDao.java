package net.coding.lib.project.dao;


import net.coding.lib.project.dto.ProgramDTO;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.parameter.ProgramProjectQueryParameter;
import net.coding.lib.project.parameter.ProgramPageQueryParameter;
import net.coding.lib.project.parameter.ProgramQueryParameter;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProgramDao extends tk.mybatis.mapper.common.Mapper<Project> {

    Project selectByIdAndTeamId(@Param("programId") Integer programId,
                                @Param("teamId") Integer teamId);

    List<ProgramDTO> selectProgramPages(ProgramPageQueryParameter parameter);

    List<Project> selectPrograms(ProgramQueryParameter parameter);

    /**
     * 查询团队内项目集下的所有项目
     */
    List<Project> selectProgramAllProjects(ProgramProjectQueryParameter parameter);

    /**
     * 某个项目集下成员所在项目
     */
    List<Project> selectProgramProjects(ProgramProjectQueryParameter parameter);

}