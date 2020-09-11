package net.coding.lib.project.service;

import net.coding.lib.project.dao.ProjectResourceSequenceDao;
import net.coding.lib.project.entity.ProjectResourceSequence;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.annotation.Resource;

@Service
public class ProjectResourceSequenceService {

    @Resource
    private ProjectResourceSequenceDao projectResourceSequenceDao;

    @Resource
    private SqlSessionTemplate sqlSessionTemplate;

    public ProjectResourceSequence getByProjectId(Integer projectId) {
        return projectResourceSequenceDao.getByProjectId(projectId);
    }

    /**
     * 调用此方法外部必须开启事务。
     * @param projectId
     * @return
     */
    public int generateProjectResourceCode(Integer projectId) {
        int result = projectResourceSequenceDao.update(projectId);
        if (result > 0) {
            int code = projectResourceSequenceDao.getCode();
            return code;
        } else {
            throw new RuntimeException("can not get new project resource code!!! maybe project id not found!!!");
        }
    }
}
