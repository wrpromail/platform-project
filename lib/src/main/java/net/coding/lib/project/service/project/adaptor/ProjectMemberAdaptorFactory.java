package net.coding.lib.project.service.project.adaptor;


import net.coding.lib.project.service.project.AbstractProjectMemberAdaptorService;

import org.springframework.stereotype.Component;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@AllArgsConstructor
public class ProjectMemberAdaptorFactory implements AdaptorFactory<AbstractProjectMemberAdaptorService> {
    private final List<AbstractProjectMemberAdaptorService> abstractProjectMemberAdaptorServices;

    @Override
    public AbstractProjectMemberAdaptorService create(Integer type) {
        return abstractProjectMemberAdaptorServices.stream()
                .filter(service -> service.pmType().equals(type))
                .findFirst()
                .orElse(null);
    }
}
