package net.coding.app.project.http;

import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.service.ProjectResourceSequenceService;
import net.coding.lib.project.service.ProjectResourceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/api/project/resources")
public class ProjectResourcesController {

    @Autowired
    private ProjectResourceService projectResourceService;

    @Autowired
    private ProjectResourceSequenceService projectResourceSequenceService;

    @GetMapping("/findProjectResourcesList")
    public String findProjectResourcesList(Integer projectId, Integer page, Integer pageSize) {
//        projectResourceService.findProjectResourceList(7);
        try {
            ProjectResource projectResource = new ProjectResource();
            projectResource.setProjectId(7);
            projectResource.setTargetType("WikiMenu");
            projectResource.setTargetId(11);
            projectResource.setCreatedBy(1);
            projectResource.setTitle("Test home");
            projectResourceService.addProjectResource(projectResource);
            if (projectId == null)
                return "error";
        } catch (Exception ex) {
            System.out.println(ex);
            return "fail";
        }
        return "success";
    }
}
