package net.coding.lib.project.service;

import com.google.common.base.Strings;

import net.coding.common.vendor.Inflector;
import net.coding.e.proto.FileProto;
import net.coding.e.proto.IssueProto;
import net.coding.lib.project.entity.Depot;
import net.coding.lib.project.entity.ExternalLink;
import net.coding.lib.project.entity.MergeRequest;
import net.coding.lib.project.entity.Project;
import net.coding.lib.project.entity.ProjectResource;
import net.coding.lib.project.entity.Release;
import net.coding.lib.project.grpc.client.FileServiceGrpcClient;
import net.coding.lib.project.grpc.client.IssueServiceGrpcClient;
import net.coding.proto.depot.DepotProto;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProjectResourceLinkService {


    private final IssueServiceGrpcClient issueServiceGrpcClient;

    private final ExternalLinkService externalLinkService;

    private final MergeRequestService mergeRequestService;

    private final ReleaseService releaseService;

    private final FileServiceGrpcClient fileServiceGrpcClient;

    private final DepotService depotService;

    private final ProjectService projectService;

    private final ProjectResourceService projectResourceService;

    public static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&#.+;", Pattern.DOTALL);

    private static final String template = "<a class=\"refer-resource-link {0}\" href=\"{1}\" refer-id=\"{2}\" target=\"_blank\">{3}</a>";

    // code 标签, 使其内容中的 #1 不转换成 resource link
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```.+?```|`.+?`|<code>.+?</code>", Pattern.DOTALL);

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(gfm-extraction-([0-9a-f]{32}))");

    public static final Pattern PROJECT_RESOURCE_PATTERN = Pattern.compile("(?<=^|\\s)+(?<projectname>[0-9a-zA-Z-_\\.]*)#(?<iid>[0-9]+)");

    // http://stackoverflow.com/questions/468370/a-regex-to-match-a-sha1
    private static final Pattern COMMIT_SHA_PATTERN = Pattern.compile("\\b(?<commitId>[0-9a-f]{7,40})\\s");

    private static final Map<String, String> typeToUrlMap = new HashMap<String, String>() {{
        put("merge-request-bean", "_buildMergeRequestLink");
        put("pull-request-bean", "git/pull");
        put("project-topic", "topic");
        put("project-file", "_buildProjectFileLink");
        put("release", "_buildReleaseLink");
        put("wiki", "wiki");
        put("defect", "defect");
        put("mission", "mission");
        put("requirement", "requirement");
        put("iteration", "iteration");
        put("sub-task", "subtask");
        put("epic", "epic");
        put("external-link", "_buildExternalLinkUrl");
        put("testing-plan-case-result", "_buildTestingPlanCaseResultUrl");
    }};

    private static final Set<String> useCodeTypeSet = new HashSet<String>() {{
        add("pull-request-bean");
        add("wiki");
    }};



    public String linkize(String content, Project project) {
        if (null == project) {
            return content;
        }
        return replace(content, project);
    }

    private String replace(String content, Project project) {
        String result = Strings.nullToEmpty(content);
        Map<String, String> placeholderMap = new HashMap<>();

        result = filterTag(result, placeholderMap, CODE_BLOCK_PATTERN);
        result = filterTag(result, placeholderMap, HTML_ENTITY_PATTERN);
        result = replaceResource(result, project);
        return restoreTag(result, placeholderMap);
    }

    private String restoreTag(String content, Map<String, String> map) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        while (matcher.find()) {
            String rpl = matcher.group(1);
            String md5 = matcher.group(2);
            if (StringUtils.isNotEmpty(rpl) && StringUtils.isNotEmpty(map.get(md5))) {
                content = content.replace(rpl, map.get(md5));
            }
        }
        return content;
    }

    private String replaceResource(String content, Project project) {
        if (project == null) {
            return content;
        }
        StringBuffer sb = new StringBuffer();
        Matcher matcher = PROJECT_RESOURCE_PATTERN.matcher(content);
        while (matcher.find()) {
            //String text = matcher.group(0);
            String projectName = matcher.group("projectname");
            if (StringUtils.isNotBlank(projectName)) {
                projectName = projectName.trim();
            }
            String resourceCode = matcher.group("iid");
            Integer number = NumberUtils.toInt(resourceCode);

            Project targetProject = StringUtils.isBlank(projectName) ? project : projectService.getByNameAndTeamId(projectName, project.getTeamOwnerId());
            if (targetProject != null) {
                ProjectResource projectResource = projectResourceService.getProjectResourceWithDeleted(targetProject.getId(), number);
                if (projectResource != null) {
                    boolean isPrefixProjectName = !targetProject.getId().equals(project.getId());
                    matcher.appendReplacement(sb, buildLink(projectResource, targetProject, isPrefixProjectName));
                }
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String buildLink(ProjectResource projectResource, Project project, boolean prefixProjectName) {
        String type = Inflector.underscore(projectResource.getTargetType()).replace('_', '-');
        String targetId = String.valueOf(projectResource.getTargetId());
        String resouceCode = prefixProjectName ?
                project.getName() + "#" + projectResource.getCode().toString()
                :
                "#" + projectResource.getCode().toString();
        return MessageFormat.format(template, type, projectResource.getResourceUrl(), targetId, resouceCode);
    }

    private String filterTag(String content, Map<String, String> map, Pattern tagReg) {
        Matcher matcher = tagReg.matcher(content);
        while (matcher.find()) {
            String pre = matcher.group(0);
            String md5 = DigestUtils.md5Hex(pre);
            content = content.replace(pre, "gfm-extraction-" + md5);
            map.put(md5, pre);
        }
        return content;
    }

    private String buildMergeRequestLink(ProjectResource projectResource, String projectPath) {
        MergeRequest mergeRequest = mergeRequestService.getById(projectResource.getTargetId());
        if (Objects.isNull(mergeRequest)) {
            return "#";
        }
        Depot depot = depotService.getById(mergeRequest.getDepotId());
        if(Objects.isNull(depot)) {
            return "#";
        }
        String depotName = depot.getName();
        return projectPath + "/d/" + depotName + "/git/merge/" + projectResource.getCode();
    }

    private String buildProjectFileLink(ProjectResource projectResource, String projectPath) {
        FileProto.File file = fileServiceGrpcClient.getProjectFileByIdWithDel(projectResource.getProjectId(), projectResource.getTargetId());
        if (null == file) {
            return "#";
        }
        Integer fileParentId = file.getParentId();
        Integer fileId = file.getId();
        return projectPath + "/attachment/" + fileParentId + "/preview/" + fileId;
    }

    private String buildReleaseLink(ProjectResource projectResource, String projectPath) {
        Release release = releaseService.getById(projectResource.getTargetId());
        if(Objects.isNull(release) || release.getTagName().startsWith("release/")) {
            return "#";
        }
        Depot depot = depotService.getById(release.getDepotId());
        if(Objects.isNull(depot)) {
            return "#";
        }
        String releaseTagName = release.getTagName();
        String depotName = depot.getName();
        return projectPath + "/d/" + depotName + "/git/releases/" + releaseTagName;
    }

    private String buildExternalLinkUrl(ProjectResource projectResource) {
        ExternalLink link = externalLinkService.getById(projectResource.getTargetId());
        if (Objects.isNull(link)) {
            return "#";
        }
        return link.getLink();
    }

    private String buildDefectLink(ProjectResource projectResource, String projectPath) {
        IssueProto.IssueResponse issue = issueServiceGrpcClient.getIssueById(projectResource.getTargetId(), projectResource.getProjectId(), false);
        if (Objects.isNull(issue.getData()) || 0 != issue.getCode()) {
            return "#";
        }
        return projectPath + "/bug-tracking/issues/" + projectResource.getCode() + "/detail";
    }

    private String buildRequirementLink(ProjectResource projectResource, String projectPath) {
        IssueProto.IssueResponse issue = issueServiceGrpcClient.getIssueById(projectResource.getTargetId(), projectResource.getProjectId(), false);
        if (Objects.isNull(issue.getData()) || 0 != issue.getCode()) {
            return "#";
        }
        return projectPath + "/requirements/issues/" + projectResource.getCode() + "/detail";
    }

    private String buildMissionLink(ProjectResource projectResource, String projectPath) {
        IssueProto.IssueResponse issue = issueServiceGrpcClient.getIssueById(projectResource.getTargetId(), projectResource.getProjectId(), false);
        if (Objects.isNull(issue.getData()) || 0 != issue.getCode()) {
            return "#";
        }
        return projectPath + "/assignments/issues/" + projectResource.getCode() + "/detail";
    }

    private String buildSubTaskLink(ProjectResource projectResource, String projectPath) {
        IssueProto.IssueResponse issue = issueServiceGrpcClient.getIssueById(projectResource.getTargetId(), projectResource.getProjectId(), false);
        if (Objects.isNull(issue.getData()) || 0 != issue.getCode()) {
            return "#";
        }
        return projectPath + "/subtasks/issues/" + projectResource.getCode() + "/detail";
    }

    private String buildEpicLink(ProjectResource projectResource, String projectPath) {
        IssueProto.IssueResponse issue = issueServiceGrpcClient.getIssueById(projectResource.getTargetId(), projectResource.getProjectId(), false);
        if (Objects.isNull(issue.getData()) || 0 != issue.getCode()) {
            return "#";
        }
        return projectPath + "/epics/issues/" + projectResource.getCode() + "/detail";
    }

    private String buildIterationLink(ProjectResource projectResource, String projectPath) {
        return projectPath + "/iterations/" + projectResource.getCode();
    }

    public String getResourceLink(ProjectResource projectResource, String projectPath) {
        String type = Inflector.underscore(projectResource.getTargetType()).replace('_', '-');
        String urlType = typeToUrlMap.get(type);
        if (null != urlType) {
            if ("_buildMergeRequestLink".equals(urlType)) {
                return buildMergeRequestLink(projectResource, projectPath);
            } else if ("_buildProjectFileLink".equals(urlType)) {
                return buildProjectFileLink(projectResource, projectPath);
            } else if ("_buildReleaseLink".equals(urlType)) {
                return buildReleaseLink(projectResource, projectPath);
            } else if ("_buildExternalLinkUrl".equals(urlType)) {
                return buildExternalLinkUrl(projectResource);
            } else if (useCodeTypeSet.contains(type)) {
                return projectPath + "/" + urlType + "/" + projectResource.getCode();
            } else if ("defect".equals(urlType)) {
                return buildDefectLink(projectResource, projectPath);
            } else if ("requirement".equals(urlType)) {
                return buildRequirementLink(projectResource, projectPath);
            } else if ("mission".equals(urlType)) {
                return buildMissionLink(projectResource, projectPath);
            } else if ("subtask".equals(urlType)) {
                return buildSubTaskLink(projectResource, projectPath);
            } else if ("epic".equals(urlType)) {
                return buildEpicLink(projectResource, projectPath);
            } else if ("iteration".equals(urlType)) {
                return buildIterationLink(projectResource, projectPath);
            } else {
                return projectPath + "/" + urlType + "/" + projectResource.getTargetId();
            }
        } else {
            return projectPath + "/" + type + "/" + projectResource.getTargetId();
        }
    }
}
