package net.coding.lib.project.service;

import net.coding.lib.project.dao.ProjectResourcesDao;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class ProjectResourcesService {

    @Resource
    private ProjectResourcesDao projectResourcesDao;


}
