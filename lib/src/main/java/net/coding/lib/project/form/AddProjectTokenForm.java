package net.coding.lib.project.form;

import com.google.common.collect.Sets;

import net.coding.common.base.form.BaseForm;
import net.coding.common.constants.DeployTokenScopeEnum;
import net.coding.lib.project.dto.ProjectTokenArtifactDTO;
import net.coding.lib.project.dto.ProjectTokenDepotDTO;


import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddProjectTokenForm extends BaseForm {

    private String tokenName;
    private String expiredAt;
    private List<ProjectTokenDepotDTO> depotScopes;
    private List<ProjectTokenArtifactDTO> artifactScopes;
    private String scope;
    @Builder.Default
    private boolean applyToAllDepots = true;
    @Builder.Default
    private boolean applyToAllArtifacts = true;

    public static final Set<String> ARTIFACT_SCOPE_SET = Sets.newHashSet(
            DeployTokenScopeEnum.ARTIFACT_R.getValue(),
            DeployTokenScopeEnum.ARTIFACT_RW.getValue(),
            DeployTokenScopeEnum.ARTIFACT_VERSION_PROPS_RW.getValue()
    );
}
