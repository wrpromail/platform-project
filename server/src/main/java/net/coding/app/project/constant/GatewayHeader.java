package net.coding.app.project.constant;

public interface GatewayHeader {
    String USER_GK = "CODING-USER-GLOBAL-KEY";
    String USER_ID = "CODING-USER-ID";
    String USER_NAME = "CODING-USER-NAME";

    String TEAM_ID = "CODING-TEAM-ID";
    String TEAM_GK = "CODING-TEAM-GLOBAL-KEY";
    String TEAM_NAME = "CODING-TEAM-NAME";

    String PROJECT_ID = "CODING-PROJECT-ID";
    String PROJECT_NAME = "CODING-PROJECT-NAME";

    String SESSION_KEY = "SESSION-HEADER-KEY";
    /**
     * 团队域名
     */
    String HOST = "X-FORWARDED-HOST";

}
