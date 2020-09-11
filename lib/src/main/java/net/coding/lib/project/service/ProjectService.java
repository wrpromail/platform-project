package net.coding.lib.project.service;

import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.entity.Project;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class ProjectService {


    @Resource
    private ProjectDao projectDao;

    public Project getById(Integer id) {
        return projectDao.getById(id);
    }
}
