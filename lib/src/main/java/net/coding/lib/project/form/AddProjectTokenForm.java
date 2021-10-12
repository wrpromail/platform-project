package net.coding.lib.project.form;

import com.google.common.collect.Sets;

import net.coding.common.base.form.BaseForm;
import net.coding.common.constants.DeployTokenScopeEnum;
import net.coding.lib.project.dto.ProjectTokenArtifactDTO;
import net.coding.lib.project.dto.ProjectTokenDepotDTO;

import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
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
