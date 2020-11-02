package net.coding.lib.project.enums;

public enum ActivityEnums {
    // Project
    ACTION_CREATE("Project", (short) 1, "create", "创建了"),
    ACTION_UPDATE("Project", (short) 2, "update", "更新了"),
    ACTION_UPDATE_NAME("Project", (short) 21, "updateName", "更新了项目地址"),
    ACTION_UPDATE_DISPLAY_NAME("Project", (short) 22, "updateDisplayName", "更新了项目名称"),
    ACTION_UPDATE_DESCRIPTION("Project", (short) 23, "updateDescription", "更新了项目描述"),
    ACTION_UPDATE_DATE("Project", (short) 3, "updateDate", "更新了项目时间"),
    ACTION_DELETE("Project", (short) 3, "delete", "删除了"),
    ACTION_TRANSFER("Project", (short) 4, "transfer", "转让给了"),
    ACTION_ARCHIVE("Project", (short) 5, "archive", "归档了"),
    ACTION_UNARCHIVE("Project", (short) 6, "unarchive", "取消归档了"),
    ACTION_TRANSFER_TO_TEAM("Project", (short) 7, "transferToTeam", "转让给了团队"),
    ACTION_TEAM_PROJECT_EXPORT("Project", (short) 8, "teamProjectExport", "将项目由团队迁出，转让给了"),
    ACTION_ENABLE_DEPOT_SHARING("Project", (short) 9, "enabledDepotSharing", "公开了仓库源代码"),
    ACTION_DISABLE_DEPOT_SHARING("Project", (short) 10, "disabledDepotSharing", "取消公开了仓库源代码"),
    ACTION_EXTERNAL_DEPOT_ADD("Project", (short) 11, "ACTION_EXTERNAL_DEPOT_ADD", "关联了代码仓库"),
    ACTION_EXTERNAL_DEPOT_DEL("Project", (short) 12, "ACTION_EXTERNAL_DEPOT_DEL", "取消了关联代码仓库"),

    //ProjectLabel
    ACTION_CREATE_LABEL("ProjectLabel", (short) 1, "add", "新建了"),
    ACTION_UPDATE_LABEL("ProjectLabel", (short) 2, "update", "更新了"),
    ACTION_DELETE_LABEL("ProjectLabel", (short) 3, "delete", "删除了"),

    //ProjectMember
    ACTION_ADD_MEMBER("ProjectMember", (short) 1, "add", "添加了"),
    ACTION_REMOVE_MEMBER("ProjectMember", (short) 2, "remove", "移除了"),
    ACTION_QUIT("ProjectMember", (short) 3, "quit", "退出了"),

    //ProjectSetting
    ACTION_OPEN_SETTING("ProjectSetting", (short) 1, "open", "开启了功能模块"),
    ACTION_CLOSE_SETTING("ProjectSetting", (short) 0, "close", "关闭了功能模块"),

    // ProjectTweet TODO 没有描述
    ACTION_TWEET_CREATE("ProjectTweet", (short) 1, "create", ""),
    ACTION_TWEET_UPDATE("ProjectTweet", (short) 2, "update", ""),
    ACTION_TWEET_DELETE("ProjectTweet", (short) 3, "delete", "");


    private final String handlerType;

    private final Short action;

    private final String actionStr;

    private final String msg;

    ActivityEnums(String handlerType, Short action, String actionStr, String msg) {
        this.handlerType = handlerType;
        this.action = action;
        this.actionStr = actionStr;
        this.msg = msg;
    }

    public Short getAction() {
        return action;
    }

    public String getActionStr() {
        return actionStr;
    }

    public String getHandlerType() {
        return handlerType;
    }

    public String getMsg() {
        return msg;
    }
}
