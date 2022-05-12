package net.coding.lib.project.service;

import com.google.common.collect.Sets;

import net.coding.common.constants.DeployTokenScopeEnum;
import net.coding.lib.project.dto.ProjectTokenArtifactDTO;
import net.coding.lib.project.exception.CoreException;
import net.coding.lib.project.form.AddProjectTokenForm;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author liuying
 * @Date 2021/1/14 10:45 上午
 * @Version 1.0
 */
@Service
@Slf4j
@AllArgsConstructor
public class ProjectTokenValidateService {

    private static final String DEPLOY_TOKEN_SCOPE_DELIMITER = ",";

    private static final Set<String> ARTIFACT_SCOPE_SET = Sets.newHashSet(
            DeployTokenScopeEnum.ARTIFACT_R.getValue(),
            DeployTokenScopeEnum.ARTIFACT_RW.getValue(),
            DeployTokenScopeEnum.ARTIFACT_VERSION_PROPS_RW.getValue()
    );


    public void validate(AddProjectTokenForm form) throws CoreException {
        if (StringUtils.isBlank(form.getTokenName())) {
            throw CoreException.of(CoreException.ExceptionType.DEPLOY_TOKEN_NAME_EMPTY);
        }

        if (form.getTokenName().trim().length() > 60) {
            throw CoreException.of(CoreException.ExceptionType.DEPLOY_TOKEN_NAME_TOO_LONG);
        }

        if (StringUtils.isBlank(form.getScope()) && form.isApplyToAllDepots() && form.isApplyToAllArtifacts()) {
            throw CoreException.of(CoreException.ExceptionType.DEPLOY_TOKEN_SCOPE_EMPTY);
        }

        if (StringUtils.isBlank(form.getScope()) && CollectionUtils.isEmpty(form.getDepotScopes()) && CollectionUtils.isEmpty(form.getArtifactScopes())) {
            throw CoreException.of(CoreException.ExceptionType.DEPLOY_TOKEN_SCOPE_EMPTY);
        }
        // 非应用到所有制品库时，scope 中不应该存在制品库权限
        if (!form.isApplyToAllArtifacts() && StringUtils.isNotBlank(form.getScope())) {
            String[] auths = form.getScope().split(DEPLOY_TOKEN_SCOPE_DELIMITER);
            if (Stream.of(auths).anyMatch(ARTIFACT_SCOPE_SET::contains)) {
                throw CoreException.of(CoreException.ExceptionType.DEPLOY_TOKEN_SCOPE_INVALID);
            }
        }

        // artifactScopes 中不应该存在非制品库权限
        if (CollectionUtils.isNotEmpty(form.getArtifactScopes())) {
            Stream<String> stream = form.getArtifactScopes().stream()
                    .map(ProjectTokenArtifactDTO::getScope)
                    .filter(org.apache.commons.lang3.StringUtils::isNotBlank)
                    .flatMap(scope -> Stream.of(scope.split(DEPLOY_TOKEN_SCOPE_DELIMITER)));
            if (stream.anyMatch(scope -> !ARTIFACT_SCOPE_SET.contains(scope))) {
                throw CoreException.of(CoreException.ExceptionType.DEPLOY_TOKEN_SCOPE_INVALID);
            }
        }

    }

}
