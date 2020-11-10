package net.coding.lib.project.service;

import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.entity.ProjectMember;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectMemberService {

    @Autowired
    private ProjectMemberDao projectMemberDao;

    public ProjectMember getById(Integer id) {
        return projectMemberDao.getById(id);
    }

    public int insert(ProjectMember projectMember) {
        return projectMemberDao.insert(projectMember);
    }

    public int update(ProjectMember projectMember) {
        return projectMemberDao.update(projectMember);
    }

    public List<ProjectMember> findList(Map<String, Object> parameter) {
        parameter.put("deletedAt", "1970-01-01 00:00:00");
        return projectMemberDao.findList(parameter);
    }

    public List<ProjectMember> findListByProjectId(Integer projectId) {
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("projectId", projectId);
        parameter.put("deletedAt", "1970-01-01 00:00:00");
        return projectMemberDao.findListByProjectId(parameter);
    }

    public ProjectMember getByProjectIdAndUserId(Integer projectId, Integer userId) {
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("projectId", projectId);
        parameter.put("userId", userId);
        parameter.put("deletedAt", "1970-01-01 00:00:00");
        return projectMemberDao.getByProjectIdAndUserId(parameter);
    }
}
