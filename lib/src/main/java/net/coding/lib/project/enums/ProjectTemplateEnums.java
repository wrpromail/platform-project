package net.coding.lib.project.enums;

import com.google.common.collect.Sets;

import net.coding.lib.project.entity.ProjectSetting;

import java.util.Set;

/**
 * 项目模版
 */
public enum ProjectTemplateEnums {

    CODE_HOST, // 代码托管
    PROJECT_MANAGE, // 项目管理
    DEV_OPS, // devOps
    DEMO_BEGIN, // 范例项目
    CHOICE_DEMAND; // 按需选择


    public Set<ProjectSetting.Code> getFunctions(DemoProjectTemplateEnums demoProjectTemplate) {
        switch (this) {
            case CODE_HOST:
                return FUNCTION_CODE_HOST;
            case PROJECT_MANAGE:
                return FUNCTION_PROJECT_MANAGE;
            case DEV_OPS:
                return FUNCTION_DEV_OPS;
            case DEMO_BEGIN:
                switch (demoProjectTemplate) {
                    case AGILE:
                        return FUNCTION_PROJECT_MANAGE;
                    case CLASSIC:
                        return FUNCTION_PROJECT_MANAGE;
                    case TESTING:
                        return FUNCTION_DEMO_TESTING_MANAGER;
                    case MOBILE:
                        // todo 以后要补充，兼容防止前端已经传了这个类型
                        return FUNCTION_PROJECT_MANAGE;
                    default:
                        return FUNCTION_DEMO_CODE_HOST;
                }
            case CHOICE_DEMAND:
                return FUNCTION_CHOICE_DEMAND;
            default:
                // 不支持的类型，直接报错提醒
                return null;
        }
    }

    /**
     * 范例项目之测试管理 模版的 的所有设置
     */
    public final static Set<ProjectSetting.Code> FUNCTION_DEMO_TESTING_MANAGER = Sets.newHashSet(
            ProjectSetting.Code.FUNCTION_AGILE_DEVELOPMENT,
            ProjectSetting.Code.FUNCTION_TEST_MANAGEMENT,
            ProjectSetting.Code.FUNCTION_WIKI,
            ProjectSetting.Code.FUNCTION_API_DOCS,
            ProjectSetting.Code.FUNCTION_FILE,
            ProjectSetting.Code.FUNCTION_KM,
            ProjectSetting.Code.FUNCTION_STATISTICS
    );

    /**
     * 代码托管模版的 的所有设置
     */
    public final static Set<ProjectSetting.Code> FUNCTION_CODE_HOST = Sets.newHashSet(
            ProjectSetting.Code.FUNCTION_CODE_MANAGEMENT,
            ProjectSetting.Code.FUNCTION_WIKI,
            ProjectSetting.Code.FUNCTION_API_DOCS,
            ProjectSetting.Code.FUNCTION_FILE,
            ProjectSetting.Code.FUNCTION_KM,
            ProjectSetting.Code.FUNCTION_STATISTICS,
            ProjectSetting.Code.FUNCTION_CODE_ANALYSIS
    );

    /**
     * 范例代码托管模版的 的所有设置
     */
    public final static Set<ProjectSetting.Code> FUNCTION_DEMO_CODE_HOST = Sets.newHashSet(
            ProjectSetting.Code.FUNCTION_CODE_MANAGEMENT,
            ProjectSetting.Code.FUNCTION_WIKI,
            ProjectSetting.Code.FUNCTION_API_DOCS,
            ProjectSetting.Code.FUNCTION_FILE,
            ProjectSetting.Code.FUNCTION_KM,
            ProjectSetting.Code.FUNCTION_CONTINUE_INTEGRATION,
            ProjectSetting.Code.FUNCTION_DEPLOYMENT_MANAGEMENT,
            ProjectSetting.Code.FUNCTION_ARTIFACT,
            ProjectSetting.Code.FUNCTION_STATISTICS,
            ProjectSetting.Code.FUNCTION_CODE_ANALYSIS
    );

    /**
     * 项目管理模版 的所有设置
     */
    public final static Set<ProjectSetting.Code> FUNCTION_PROJECT_MANAGE = Sets.newHashSet(
            ProjectSetting.Code.FUNCTION_AGILE_DEVELOPMENT,
            ProjectSetting.Code.FUNCTION_WIKI,
            ProjectSetting.Code.FUNCTION_API_DOCS,
            ProjectSetting.Code.FUNCTION_FILE,
            ProjectSetting.Code.FUNCTION_KM,
            ProjectSetting.Code.FUNCTION_STATISTICS,
            ProjectSetting.Code.FUNCTION_COMPASS

    );

    /**
     * devOps模版 的所有设置
     */
    public final static Set<ProjectSetting.Code> FUNCTION_DEV_OPS = Sets.newHashSet(
            ProjectSetting.Code.FUNCTION_AGILE_DEVELOPMENT,
            ProjectSetting.Code.FUNCTION_TEST_MANAGEMENT,
            ProjectSetting.Code.FUNCTION_CODE_MANAGEMENT,
            ProjectSetting.Code.FUNCTION_CONTINUE_INTEGRATION,
            ProjectSetting.Code.FUNCTION_DEPLOYMENT_MANAGEMENT,
            ProjectSetting.Code.FUNCTION_ARTIFACT,
            ProjectSetting.Code.FUNCTION_WIKI,
            ProjectSetting.Code.FUNCTION_KM,
            ProjectSetting.Code.FUNCTION_CODE_ANALYSIS,
            ProjectSetting.Code.FUNCTION_API_DOCS,
            ProjectSetting.Code.FUNCTION_QTA,
            ProjectSetting.Code.FUNCTION_STATISTICS,
            ProjectSetting.Code.FUNCTION_FILE,
            ProjectSetting.Code.FUNCTION_COMPASS,
            ProjectSetting.Code.FUNCTION_APP_OPS
    );

    public final static Set<ProjectSetting.Code> FUNCTION_CHOICE_DEMAND = Sets.newHashSet(
    );
}
