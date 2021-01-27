package net.coding.lib.project.service;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectMemberDao;
import net.coding.lib.project.entity.ProjectMember;

import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberDao projectMemberDao;

    public ProjectMember getById(Integer id) {
        return projectMemberDao.getById(id);
    }

    public int insert(ProjectMember projectMember) {
        return projectMemberDao.insert(projectMember);
    }

    public int update(ProjectMember projectMember) {
        return projectMemberDao.update(projectMember);
    }

    public List<ProjectMember> findListByProjectId(Integer projectId) {
        return projectMemberDao.findListByProjectId(projectId, Timestamp.valueOf(BeanUtils.NOT_DELETED_AT));
    }

    public ProjectMember getByProjectIdAndUserId(Integer projectId, Integer userId) {
        return projectMemberDao.getByProjectIdAndUserId(projectId, userId, Timestamp.valueOf(BeanUtils.NOT_DELETED_AT));
    }
}
