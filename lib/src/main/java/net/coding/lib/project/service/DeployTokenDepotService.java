package net.coding.lib.project.service;


import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.DeployTokenDepotsDao;

import net.coding.lib.project.entity.DeployTokenDepot;

import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author liuying
 * @Date 2021/1/7 10:56 上午
 * @Version 1.0
 */
@Service
@Slf4j
@AllArgsConstructor
public class DeployTokenDepotService {

    private final DeployTokenDepotsDao deployTokenDepotsDao;


    public List<DeployTokenDepot> getTokenById(Integer deployTokenId) {
        return deployTokenDepotsDao.getDeployTokenDepot(deployTokenId, Timestamp.valueOf(BeanUtils.NOT_DELETED_AT));
    }

    public int deleteByTokenId(Integer deployTokenId) {
        return deployTokenDepotsDao.deleteByDeployTokenDepot(deployTokenId, Timestamp.valueOf(BeanUtils.NOT_DELETED_AT));

    }

}
