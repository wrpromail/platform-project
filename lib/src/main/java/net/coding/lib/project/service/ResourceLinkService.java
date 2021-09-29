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
import net.coding.lib.project.enums.GlobalResourceTypeEnum;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ResourceLinkService {

    public String getResourceLink(ProjectResource record) {
        GlobalResourceTypeEnum globalResourceTypeEnum = GlobalResourceTypeEnum.valueFrom(record.getTargetType());
        String resourceLink;
        switch (Objects.requireNonNull(globalResourceTypeEnum)){
            case KNOWLEDGE_MANAGE:
                resourceLink = "/api/km/v1/spaces/pages/" + record.getTargetId();
                break;

            default: resourceLink = "";
        }
        return resourceLink;
    }
}
