package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ProfanityWord;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProfanityWordDao {

    List<ProfanityWord> findAll();
}
