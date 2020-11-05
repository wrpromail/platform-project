package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ExternalLink;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExternalLinkDao {
    ExternalLink getById(@Param("id") Integer id);

    int insert(ExternalLink record);

    int update(ExternalLink record);
}
