package net.coding.lib.project.form.credential;

import org.hibernate.validator.constraints.Length;

import javax.validation.Valid;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = false)
public class AndroidCredentialForm extends BaseCredentialForm {
    private String content;
    private String fileName;

    @Valid
    @Length(max = 40, message = "credential_password_too_long")
    private String filePassword;

    private String alias;

    @Valid
    @Length(max = 40, message = "credential_password_too_long")
    private String aliasPassword;
}
