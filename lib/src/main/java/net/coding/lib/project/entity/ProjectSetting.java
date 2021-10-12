package net.coding.lib.project.entity;

import com.google.common.collect.Sets;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static net.coding.common.i18n.utils.LocaleMessageSourceUtil.getMessage;

@EqualsAndHashCode(callSuper = false)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectSetting {

    public final static String valueTrue = "1";
    public final static String valueFalse = "0";

    public final static Short open = 1;
    public final static Short close = 0;

    private static final long serialVersionUID = -1973992627485106043L;

    private Integer id;
    private Integer projectId;

    private String code;

    private String value;

    private String description;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private Timestamp deletedAt;

    /**
     * 项目功能 的所有设置
     */
    public final static Set<Code> TOTAL_PROJECT_FUNCTION = Sets.newHashSet(
            Code.FUNCTION_AGILE_DEVELOPMENT,
            Code.FUNCTION_TEST_MANAGEMENT,
            Code.FUNCTION_CODE_MANAGEMENT,
            Code.FUNCTION_CONTINUE_INTEGRATION,
            Code.FUNCTION_DEPLOYMENT_MANAGEMENT,
            Code.FUNCTION_APP_OPS,
            Code.FUNCTION_ARTIFACT,
            Code.FUNCTION_WIKI,
            Code.FUNCTION_FILE,
            Code.FUNCTION_KM,
            Code.FUNCTION_STATISTICS,
            Code.FUNCTION_OLD_TASK,
            Code.FUNCTION_CODE_ANALYSIS,
            Code.FUNCTION_API_DOCS,
            Code.FUNCTION_QTA
    );

    /**
     * 敏捷特性 项目配置
     */
    public final static Set<Code> AGILE_FEATURE_MODULES = Sets.newHashSet(
            Code.AGILE_EPIC,
            Code.AGILE_REQUIREMENT,
            Code.AGILE_MISSION,
            Code.AGILE_DEFECT
    );

    public final static String getValue(boolean value) {
        return value ? valueTrue : valueFalse;
    }

    /**
     * 枚举：项目设置项
     */
    public enum Code {
        TASK_HIDE("task_hide", "old_task_hidden", valueFalse, valueFalse),
        ITERATION_MODULE_SWITCH("iteration_module_switch", "open_ierator", valueFalse, valueFalse),
        EPIC_WELCOME("epic_welcome", "open_epic", valueFalse, valueFalse),
        PROJECT_TEMPLATE_TYPE("project_template_type", "project_module_type", valueFalse, valueFalse),
        DEMO_TEMPLATE_TYPE("demo_template_type", "example_module_type", valueFalse, valueFalse),

        FUNCTION_AGILE_DEVELOPMENT("agile_development", "agile_project_development", valueTrue, valueTrue),
        FUNCTION_TEST_MANAGEMENT("test_management", "test_manage", valueTrue, valueTrue),
        FUNCTION_CODE_MANAGEMENT("code_management", "code_deposit", valueTrue, valueTrue),
        FUNCTION_CONTINUE_INTEGRATION("continue_integration", "ci", valueTrue, valueTrue),
        FUNCTION_DEPLOYMENT_MANAGEMENT("deployment_management", "deployment_manage", valueTrue, valueTrue),
        FUNCTION_APP_OPS("app_ops", "app_ops", valueTrue, valueTrue),
        FUNCTION_ARTIFACT("artifact", "artifact", valueTrue, valueTrue),
        FUNCTION_WIKI("wiki", "wiki", valueTrue, valueTrue),
        FUNCTION_STATISTICS("statistics", "statistics", valueTrue, valueFalse),
        FUNCTION_OLD_TASK("old_task", "task_old", valueTrue, valueFalse),
        FUNCTION_CODE_ANALYSIS("code_analysis", "code_analysis", valueTrue, valueTrue),
        FUNCTION_API_DOCS("api_docs", "api_doc", valueTrue, valueTrue),
        FUNCTION_QTA("qta", "test_auto", valueTrue, valueTrue),
        FUNCTION_FILE("file", "file_pan", valueTrue, valueTrue),
        FUNCTION_KM("knowledge", "knowledge", valueTrue, valueTrue),

        AGILE_ITERATION("agile_development_iteration", "agile_develop_iteration", valueTrue, valueTrue),
        AGILE_EPIC("agile_development_epic", "agile_develop_epic", valueFalse, valueFalse),
        AGILE_REQUIREMENT("agile_development_requirement", "agile_develop_demand", valueFalse, valueFalse),
        AGILE_MISSION("agile_development_mission", "agile_develop_task", valueFalse, valueFalse),
        AGILE_DEFECT("agile_development_defect", "agile_develop_flaw", valueFalse, valueFalse),
        AGILE_STORY_POINT("agile_story_point", "agile_story_point", valueFalse, valueFalse),
//        AGILE_STORY_POINT_TYPE("agile_story_point_type", "agile_story_point_type", StoryPointService.STORY_POINT_TYPE.FIBONACCI.toString(), StoryPointService.STORY_POINT_TYPE.FIBONACCI.toString()),
//        COOPERATE_MODE("cooperate_mode", "cooperate_mode", ProjectSettingService.CooperateMode.SCRUM.name(), ProjectSettingService.CooperateMode.SCRUM.name()),

        DAILY_WORK_EMAIL_NOTIFICATION("daily_work_email_notification", "daily_work_email_notification", valueFalse, valueFalse);;
        private String code;
        private String description;
        private String defaultValue;
        private String oaDefaultValue;

        Code(String code, String description, String defaultValue, String oaDefaultValue) {
            this.code = code;
            this.description = description;
            this.defaultValue = defaultValue;
            this.oaDefaultValue = oaDefaultValue;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return getMessage(description);
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public String getOaDefaultValue() {
            return oaDefaultValue;
        }

        public static net.coding.e.lib.core.bean.ProjectSetting.Code getByCode(String code) {
            return  Arrays.stream(net.coding.e.lib.core.bean.ProjectSetting.Code.values()).
                    filter(c -> c.getCode().equals(code))
                    .findFirst()
                    .orElse(null);
        }
    }

}
