package net.coding.lib.project.form;

import net.coding.common.base.form.BaseForm;
import net.coding.common.base.validator.IncludeProfanity;
import net.coding.common.base.validator.IsNumber;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


import static net.coding.common.base.validator.ValidationConstants.PROJECT_DESCRIPTION_MAX_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_NAME_MAX_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_NAME_MIN_LENGTH;

/**
 * User: Michael Chen Email: yidongnan@gmail.com Date: 2014/3/25 Time: 18:03
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UpdateProjectForm extends BaseForm {

    @NotNull(message = "param_error")
    @IsNumber(message = "param_error")
    private String id;

    @NotEmpty(message = "project_name_not_empty")
    @Length(min = PROJECT_NAME_MIN_LENGTH, max = PROJECT_NAME_MAX_LENGTH, message = "project_name_length_error")
    @IncludeProfanity(message = "content_include_sensitive_words")
    private String name;

    // 保持 API 的兼容性，DisplayName 暂时是可选字段，Null 或者 "" 表示不更新
    // 由前端保证必填，如果 Present 则检查合法性（长度，重复）
    // 当 APP 端也有相应的更新之后，这里可改成必填
    @IncludeProfanity(message = "content_include_sensitive_words")
    private String displayName;

    @Length(max = PROJECT_DESCRIPTION_MAX_LENGTH, message = "project_description_too_long")
    @IncludeProfanity(message = "content_include_sensitive_words")
    private String description;
    private String startDate;
    private String endDate;
}
