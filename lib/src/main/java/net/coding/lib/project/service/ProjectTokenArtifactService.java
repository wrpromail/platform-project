package net.coding.lib.project.service;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectTokenArtifactDao;
import net.coding.lib.project.entity.ProjectTokenArtifact;

import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

import lombok.AllArgsConstructor;

/**
 * @Author liuying
 * @Date 2021/1/11 2:57 下午
 * @Version 1.0
 */
@Service
@AllArgsConstructor
public class ProjectTokenArtifactService {

    private final ProjectTokenArtifactDao projectTokenArtifactDao;


    public List<ProjectTokenArtifact> getByTokenId(Integer deployTokenId) {
        return projectTokenArtifactDao.getProjectTokenArtifacts(deployTokenId, Timestamp.valueOf(BeanUtils.NOT_DELETED_AT));
    }

    public int deleteByTokenId(Integer deployTokenId) {
        return projectTokenArtifactDao.deleteByProjectTokenArtifacts(deployTokenId, Timestamp.valueOf(BeanUtils.NOT_DELETED_AT));
    }
}
