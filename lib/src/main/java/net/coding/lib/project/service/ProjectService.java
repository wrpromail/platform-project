package net.coding.lib.project.service;

import net.coding.lib.project.dao.ProjectDao;
import net.coding.lib.project.entity.Project;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

@Service
public class ProjectService {

    @Resource
    private ProjectDao projectDao;

    public Project getById(Integer id) {
        return projectDao.getById(id);
    }

    public Project getByNameAndTeamId(String projectName, Integer teamOwnerId) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectName", projectName);
        parameters.put("teamOwnerId", teamOwnerId);
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return projectDao.getByNameAndTeamId(parameters);
    }
}
