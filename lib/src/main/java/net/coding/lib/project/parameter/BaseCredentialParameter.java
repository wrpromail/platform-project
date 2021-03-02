package net.coding.lib.project.parameter;

import net.coding.common.base.form.BaseForm;
import net.coding.common.base.validator.EnumValid;
import net.coding.common.base.validator.StringEnumeration;
import net.coding.lib.project.dto.ConnectionTaskDTO;
import net.coding.lib.project.enums.ConnGenerateByEnums;
import net.coding.lib.project.enums.CredentialScopeEnums;
import net.coding.lib.project.enums.CredentialTypeEnums;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import java.util.List;

import javax.validation.Valid;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 抽象出 BaseCredentialForm，原有 CredentialForm 继承这个类，不做更多修改。 新增的类型如 AndroidCredentialForm 直接继承
 * BaseCredentialForm，防止按照原有的 CredentialsForm 过于臃肿。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class BaseCredentialParameter extends BaseForm {
    private int id;

    private int projectId;

    private int creatorId;

    private int teamId;

    private String credentialId;

    private ConnGenerateByEnums connGenerateBy;

    /**
     * PROJECT(1),PRIVATE(2)
     */
    @Valid
    @EnumValid(enumClasses = CredentialScopeEnums.class, message = "ci_credential_scope_invalid")
    private int scope = CredentialScopeEnums.PROJECT.getCode();

    @Valid
    @NotBlank
    @Length(max = 30, message = "ci_certificate_name_too_long")
    private String name = "";

    @Valid
    @Length(max = 100, message = "ci_certificate_description_too_long")
    private String description = "";

    @Valid
    @NotBlank
    @StringEnumeration(enumClass = CredentialTypeEnums.class, message = "ci_credential_type_invalid")
    private String type = "";

    private List<ConnectionTaskDTO> taskDTOS;

    private boolean allSelect = false;
}
