package net.coding.lib.project.service.project;

import net.coding.lib.project.service.adaptor.AdaptorFactory;

import org.springframework.stereotype.Component;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@AllArgsConstructor
public class ProjectAdaptorFactory implements AdaptorFactory<AbstractProjectAdaptorService> {
    private final List<AbstractProjectAdaptorService> abstractProjectAdaptorServices;

    @Override
    public AbstractProjectAdaptorService create(Integer type) {
        return abstractProjectAdaptorServices.stream()
                .filter(service -> service.pmType().equals(type))
                .findFirst()
                .orElse(null);
    }
}
