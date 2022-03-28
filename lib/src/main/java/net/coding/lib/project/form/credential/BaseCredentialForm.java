package net.coding.lib.project.form.credential;

import net.coding.common.base.validator.EnumValid;
import net.coding.common.base.validator.StringEnumeration;
import net.coding.lib.project.dto.ConnectionTaskDTO;
import net.coding.lib.project.credential.enums.CredentialGenerated;
import net.coding.lib.project.credential.enums.CredentialScope;
import net.coding.lib.project.credential.enums.CredentialType;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import java.util.List;

import javax.validation.Valid;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public abstract class BaseCredentialForm {
    private int id;

    private int projectId;

    private int creatorId;

    private int teamId;

    private String credentialId;

    private CredentialGenerated connGenerateBy;

    /**
     * PROJECT(1),PRIVATE(2)
     */
    @Valid
    @EnumValid(enumClasses = CredentialScope.class, message = "credential_scope_invalid")
    private int scope = CredentialScope.PROJECT.getCode();

    @Valid
    @NotBlank
    @Length(max = 30, message = "certificate_name_too_long")
    private String name;

    @Valid
    @Length(max = 100, message = "certificate_description_too_long")
    private String description;

    @Valid
    @NotBlank
    @StringEnumeration(enumClass = CredentialType.class, message = "credential_type_invalid")
    private String type;

    private List<ConnectionTaskDTO> taskDTOS;

    private boolean allSelect = false;
}
