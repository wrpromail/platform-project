package net.coding.lib.project.dao;


import net.coding.lib.project.entity.ProjectResource;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProjectResourceDao {

    int insert(ProjectResource record);

    int update(ProjectResource record);

    ProjectResource selectById(@Param("id") Integer id);

    List<ProjectResource> findList(Map<String, Object> parameter);

    ProjectResource getByProjectIdAndTypeAndTarget(Map<String, Object> parameter);

    int batchDelete(Map<String, Object> parameter);

    ProjectResource findByProjectIdAndCode(Map<String, Object> parameter);

    int batchInsert(List<ProjectResource> list);

    int countByProjectIdAndCodes(Map<String, Object> parameter);
}
