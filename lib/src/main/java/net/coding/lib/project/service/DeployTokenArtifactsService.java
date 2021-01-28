package net.coding.lib.project.service;

import net.coding.lib.project.dao.DeployTokenArtifactsDao;
import net.coding.lib.project.entity.DeployTokenArtifacts;

import org.springframework.stereotype.Service;

import java.util.List;

import lombok.AllArgsConstructor;

/**
 * @Author liuying
 * @Date 2021/1/11 2:57 下午
 * @Version 1.0
 */
@Service
@AllArgsConstructor
public class DeployTokenArtifactsService {

    private final DeployTokenArtifactsDao deployTokenArtifactsDao;


    public List<DeployTokenArtifacts> getByTokenId(Integer deployTokenId) {
        return deployTokenArtifactsDao.getDeployTokenArtifacts(deployTokenId);
    }

    public int deleteByTokenId(Integer deployTokenId) {
        return deployTokenArtifactsDao.deleteByDeployTokenArtifacts(deployTokenId);
    }
}
