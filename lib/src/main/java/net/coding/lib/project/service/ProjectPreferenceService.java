package net.coding.lib.project.service;

import net.coding.lib.project.dao.ProjectPreferenceDao;
import net.coding.lib.project.entity.ProjectPreference;
import net.coding.lib.project.enums.NotSearchTargetTypeEnum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProjectPreferenceService {

    @Autowired
    private ProjectPreferenceDao projectPreferenceDao;

    public ProjectPreference getById(Integer id) {
        return projectPreferenceDao.getById(id);
    }

    public int insert(ProjectPreference projectPreference) {
        return projectPreferenceDao.insert(projectPreference);
    }

    public int update(ProjectPreference projectPreference) {
        return projectPreferenceDao.update(projectPreference);
    }

    public List<ProjectPreference> findList(Map<String, Object> param) {
        return projectPreferenceDao.findList(param);
    }

    public ProjectPreference getByProjectIdAndType(Integer projectId, Short type) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("type", type.intValue());
        parameters.put("deletedAt", "1970-01-01 00:00:00");
        return projectPreferenceDao.getByProjectIdAndType(parameters);
    }

}
