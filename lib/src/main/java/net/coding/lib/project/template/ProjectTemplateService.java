package net.coding.lib.project.template;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class ProjectTemplateService {
    private final ProjectTemplateProperties projectTemplateProperties;

    public Set<String> getFunctions(ProjectTemplateType template, ProjectTemplateDemoType demo) {
        switch (template) {
            case DEV_OPS:
                return projectTemplateProperties.getDevOps();
            case DEMO_BEGIN:
                switch (demo) {
                    case AGILE:
                    case CLASSIC:
                    case MOBILE:
                        return projectTemplateProperties.getProjectManage();
                    case TESTING:
                        return projectTemplateProperties.getTestingManage();
                    default:
                        return projectTemplateProperties.getCodeManage();
                }
            case CHOICE_DEMAND:
            default:
                return Collections.emptySet();
        }
    }

    public boolean isInitDepot(ProjectTemplateType template, ProjectTemplateDemoType demo) {
        switch (template) {
            case PROJECT_MANAGE:
            case DEV_OPS:
            case CODE_HOST:
            case CHOICE_DEMAND:
                return false;
            case DEMO_BEGIN:
                switch (demo) {
                    case AGILE:
                    case TESTING:
                    case CLASSIC:
                        return false;
                }
            default:
                return true;
        }
    }
}
