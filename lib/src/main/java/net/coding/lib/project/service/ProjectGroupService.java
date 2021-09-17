package net.coding.lib.project.service;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectGroupDao;
import net.coding.lib.project.entity.ProjectGroup;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectGroupService {

    private final ProjectGroupDao projectGroupDao;

    public ProjectGroup getById(Integer id) {
        return projectGroupDao.selectOne(ProjectGroup.builder()
                .id(id)
                .deletedAt(BeanUtils.getDefaultDeletedAt())
                .build());
    }
}
