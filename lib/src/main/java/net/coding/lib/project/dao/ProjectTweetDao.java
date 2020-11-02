package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ProjectTweet;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ProjectTweetDao {

    int insert(ProjectTweet record);

    int update(ProjectTweet record);

    ProjectTweet getById(@Param("id") Integer id);

    List<ProjectTweet> findList(Map<String, Object> parameter);

    ProjectTweet getLast(Map<String, Object> parameter);

    ProjectTweet getLastTweetInTenMinutes(Map<String, Object> parameter);
}