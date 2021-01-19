package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ProjectSetting;

import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

/**
 * @Author liuying
 * @Date 2021/1/5 1:56 下午
 * @Version 1.0
 */
@Mapper
public interface ProjectSettingsDao {

    ProjectSetting findProjectSetting(ProjectSetting projectSetting);

    int insert(ProjectSetting projectSetting);

    int update(ProjectSetting projectSetting);

}
