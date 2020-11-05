package net.coding.lib.project.dao;

import net.coding.lib.project.entity.Depot;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DepotDao {

    Depot getById(@Param("id") Integer id);
}
