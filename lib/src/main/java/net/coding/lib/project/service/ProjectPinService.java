package net.coding.lib.project.service;

import net.coding.common.util.BeanUtils;
import net.coding.lib.project.dao.ProjectPinDao;
import net.coding.lib.project.entity.ProjectPin;

import org.springframework.stereotype.Service;


import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
@AllArgsConstructor
public class ProjectPinService {
    private final ProjectPinDao projectPinDao;

    public Optional<ProjectPin> getByProjectIdAndUserId(int projectId, int userId) {
        return Optional.ofNullable(projectPinDao.selectOne(ProjectPin.builder()
                .projectId(projectId)
                .userId(userId)
                .deletedAt(BeanUtils.getDefaultDeletedAt())
                .build()));
    }
}
