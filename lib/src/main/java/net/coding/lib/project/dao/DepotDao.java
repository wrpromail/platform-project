package net.coding.lib.project.dao;

import net.coding.lib.project.entity.Depot;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DepotDao {

    Depot getById(@Param("id") Integer id);

    List<Depot> getByIds(@Param("list") List<Integer> ids);
}
