package net.coding.lib.project.service;


import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectTokenDepotDao;
import net.coding.lib.project.entity.ProjectTokenDepot;

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
public class ProjectTokenDepotService {

    private final ProjectTokenDepotDao projectTokenDepotDao;


    public List<ProjectTokenDepot> getTokenById(Integer deployTokenId) {
        return projectTokenDepotDao.getProjectTokenDepot(deployTokenId, Timestamp.valueOf(BeanUtils.NOT_DELETED_AT));
    }

    public int deleteByTokenId(Integer deployTokenId) {
        return projectTokenDepotDao.deleteByProjectTokenDepot(deployTokenId, Timestamp.valueOf(BeanUtils.NOT_DELETED_AT));

    }

}
