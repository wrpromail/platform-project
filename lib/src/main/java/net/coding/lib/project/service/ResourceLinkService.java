package net.coding.lib.project.service;

import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.enums.GlobalResourceTypeEnum;

import org.springframework.stereotype.Service;

import java.util.Objects;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ResourceLinkService {

    public String getResourceLink(ProjectResource record) {
        GlobalResourceTypeEnum globalResourceTypeEnum = GlobalResourceTypeEnum.valueFrom(record.getTargetType());
        switch (Objects.requireNonNull(globalResourceTypeEnum)) {
            case KNOWLEDGE_MANAGE:
                return "/api/km/v1/spaces/pages/" + record.getTargetId();
            case GLOBAL_REQUIREMENT:
                return "/requirements/issues/" + record.getCode() + "/detail";
            default:
                return "";
        }
    }
}
