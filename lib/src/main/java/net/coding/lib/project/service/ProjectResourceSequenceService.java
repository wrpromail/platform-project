package net.coding.lib.project.service;

import net.coding.lib.project.dao.ProjectResourceSequenceDao;
import net.coding.lib.project.entity.ProjectResourceSequence;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

@Service
public class ProjectResourceSequenceService {

    @Resource
    private ProjectResourceSequenceDao projectResourceSequenceDao;

    public ProjectResourceSequence getByProjectId(Integer projectId) {
        return projectResourceSequenceDao.getByProjectId(projectId);
    }

    /**
     * 调用此方法外部必须开启事务。
     *
     * @param projectId
     * @return
     */
    public int generateProjectResourceCode(Integer projectId) {

        if (projectResourceSequenceDao.getByProjectId(projectId) == null) {
            addProjectResourceSequence(ProjectResourceSequence.builder().code(0).projectId(projectId).build());
        }
        int result = projectResourceSequenceDao.generateProjectResourceCode(projectId);
        if (result > 0) {
            int code = projectResourceSequenceDao.getCode();
            return code;
        } else {
            throw new RuntimeException("can not get new project resource code!!! maybe project id not found!!!");
        }
    }

    /**
     * 调用此方法外部必须开启事务。
     *
     * @param projectId
     * @return
     */
    public int generateProjectResourceCodes(Integer projectId, Integer codeAmount) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("codeAmount", codeAmount);
        if (projectResourceSequenceDao.getByProjectId(projectId) == null) {
            addProjectResourceSequence(ProjectResourceSequence.builder().code(0).projectId(projectId).build());
        }
        int result = projectResourceSequenceDao.generateProjectResourceCodes(parameters);
        if (result > 0) {
            return projectResourceSequenceDao.getCodes();
        } else {
            throw new RuntimeException("can not get new project resource code!!! maybe project id not found!!!");
        }
    }

    public int addProjectResourceSequence(ProjectResourceSequence projectResourceSequence) {
        return projectResourceSequenceDao.insert(projectResourceSequence);
    }
}
