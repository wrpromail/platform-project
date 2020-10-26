package net.coding.lib.project.dao;

import net.coding.lib.project.entity.ProfanityWord;

import java.util.List;

public interface ProfanityWordDao {

    List<ProfanityWord> findAll();
}
