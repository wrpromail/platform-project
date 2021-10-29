package net.coding.lib.project.form;

import net.coding.common.base.form.BaseForm;
import net.coding.common.base.validator.IncludeProfanity;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.validation.Errors;

import java.util.Arrays;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static net.coding.common.base.validator.ValidationConstants.PROJECT_DESCRIPTION_MAX_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_DISPLAY_NAME_MAX_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_DISPLAY_NAME_MIN_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_NAME_MAX_LENGTH;
import static net.coding.common.base.validator.ValidationConstants.PROJECT_NAME_MIN_LENGTH;

@ApiModel("创建项目表单")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectForm extends BaseForm {

    @ApiModelProperty(value = "项目标识", required = true)
    @NotEmpty(message = "project_name_not_empty")
    @Length(min = PROJECT_NAME_MIN_LENGTH, max = PROJECT_NAME_MAX_LENGTH, message = "project_name_length_error")
    @IncludeProfanity(message = "content_include_sensitive_words")
    private String name;

    @ApiModelProperty(value = "项目名称", required = true)
    @NotEmpty(message = "project_display_name_is_empty")
    @Length(min = PROJECT_DISPLAY_NAME_MIN_LENGTH, max = PROJECT_DISPLAY_NAME_MAX_LENGTH, message = "project_display_name_length_error")
    @IncludeProfanity(message = "content_include_sensitive_words")
    private String displayName;

    @ApiModelProperty("项目描述")
    @Length(max = PROJECT_DESCRIPTION_MAX_LENGTH, message = "project_description_too_long")
    @IncludeProfanity(message = "content_include_sensitive_words")
    private String description;

    @ApiModelProperty(value = "项目模版", required = true)
    @NotEmpty(message = "project_template_not_empty")
    private String projectTemplate;

    @ApiModelProperty("示例项目模版")
    private String template;

    @ApiModelProperty(value = "项目封面", required = true)
    @NotEmpty(message = "project_icon_not_empty")
    private String icon;

    @ApiModelProperty("凭据类型")
    private String credentialType;

    @ApiModelProperty("项目分组id")
    private Integer groupId;

    @ApiModelProperty("所需功能模块")
    private List<ProjectFunction> functionModule;

    public enum ProjectFunction {
        FUNCTION_AGILE_DEVELOPMENT("agile_development", "agile_project_development"),
        FUNCTION_TEST_MANAGEMENT("test_management", "test_manage"),
        FUNCTION_CODE_MANAGEMENT("code_management", "code_deposit"),
        FUNCTION_CONTINUE_INTEGRATION("continue_integration", "ci"),
        FUNCTION_DEPLOYMENT_MANAGEMENT("deployment_management", "deployment_manage"),
        FUNCTION_APP_OPS("app_ops", "app_ops"),
        FUNCTION_ARTIFACT("artifact", "artifact"),
        FUNCTION_WIKI("wiki", "wiki"),
        FUNCTION_STATISTICS("statistics", "statistics"),
        FUNCTION_CODE_ANALYSIS("code_analysis", "code_analysis"),
        FUNCTION_API_DOCS("api_docs", "api_doc"),
        FUNCTION_QTA("qta", "test_auto"),
        FUNCTION_FILE("file", "file_pan"),
        FUNCTION_KM("knowledge", "knowledge"),
        FUNCTION_COMPASS("compass", "compass");
        private String code;
        private String description;

        ProjectFunction(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public static ProjectFunction codeOf(String code) {
            return Arrays.stream(values())
                    .filter(value -> value.code.equals(code))
                    .findFirst()
                    .orElse(null);
        }


    }


    @Override
    public boolean validate(Errors errors) {
        return super.validate(errors);
    }
}
