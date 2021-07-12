package net.coding.lib.project.form;

import net.coding.common.base.form.BaseForm;
import net.coding.common.base.validator.IncludeProfanity;
import net.coding.common.base.validator.ProfanityValidate;
import net.coding.lib.project.enums.ProgramWorkflowEnums;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.validation.Errors;

import java.util.Set;

import javax.validation.constraints.Pattern;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static net.coding.common.base.validator.ValidationConstants.PROJECT_DESCRIPTION_MAX_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_DISPLAY_NAME_MAX_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_DISPLAY_NAME_MIN_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_NAME_MAX_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_NAME_MIN_LENGTH;
import static net.coding.common.constants.ProjectConstants.PROJECT_DISPLAY_NAME_REGEX;
import static net.coding.common.constants.ProjectConstants.PROJECT_NAME_REGEX;

@ApiModel("创建项目集表单")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CreateProgramForm extends BaseForm {

    @ApiModelProperty(value = "项目集标识", required = true)
    @NotEmpty(message = "project_name_not_empty")
    @Length(min = PROJECT_NAME_MIN_LENGTH, max = PROJECT_NAME_MAX_LENGTH, message = "project_name_length_error")
    @IncludeProfanity(message = "content_include_sensitive_words")
    @Pattern(regexp = PROJECT_NAME_REGEX, message = "project_name_error")
    private String name;

    @ApiModelProperty(value = "项目集名称", required = true)
    @NotEmpty(message = "project_display_name_is_empty")
    @Length(min = PROJECT_DISPLAY_NAME_MIN_LENGTH, max = PROJECT_DISPLAY_NAME_MAX_LENGTH, message = "project_display_name_length_error")
    @IncludeProfanity(message = "content_include_sensitive_words")
    @Pattern(regexp = PROJECT_DISPLAY_NAME_REGEX, message = "project_display_name_error")
    private String displayName;

    @ApiModelProperty("项目集描述")
    @Length(max = PROJECT_DESCRIPTION_MAX_LENGTH, message = "project_description_too_long")
    @IncludeProfanity(message = "content_include_sensitive_words")
    private String description;

    @ApiModelProperty(value = "封面")
    private String icon;

    @ApiModelProperty(value = "开始时间")
    private String startDate;

    @ApiModelProperty(value = "结束时间")
    private String endDate;

    @ApiModelProperty(value = "工作流选项")
    @Builder.Default
    private ProgramWorkflowEnums programWorkflow = ProgramWorkflowEnums.TEMPLATE;

    @ApiModelProperty(value = "已有工作流项目集Id")
    private Integer workflowProgramId;

    @Override
    public void validate(Object target, Errors errors) {
        //敏感词校验
        ProfanityValidate.process(errors, this);
    }
}
