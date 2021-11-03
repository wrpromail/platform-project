package net.coding.lib.project.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

public enum TemplateEnums {
    TENCENT_SLS_VUE(
            "tencent-fullstack-vue",
            "depotName",
            "coding.net"
    ),
    TENCENT_SLS_DB(
            "tencent-fullstack-serverless-db",
            "depotName",
            "coding.net"
    ),
    SPRING(
            "spring-demo",
            "depotName",
            "coding.net"
    ),
    ROR(
            "ror-demo",
            "depotName",
            "coding.net"
    ),
    SINATRA(
            "sinatra-demo",
            "depotName",
            "coding.net"
    ),
    NODEJS(
            "nodejs-demo",
            "depotName",
            "coding.net"
    ),
    ANDROID(
            "android-demo",
            "depotName",
            "coding.net"
    ),
    FLASK(
            "flask-demo",
            "depotName",
            "coding.net"
    ),
    CD_DEMO(
            "cd-demo",
            "depotName",
            "coding.net"
    ),
    GIT_SCM(
            "coding-code-guide",
            "depotName",
            "coding.net"),
    DEVOPS_DEMO(
            "coding-devops-guide",
            "depotName",
            "coding.net"),
    TENCENT_SLS_EXPRESS(
            "tencent-serverless-express",
            "depotName",
            "coding.net"
    ),
    TENCENT_SLS_FLASK(
            "tencent-serverless-flask",
            "depotName",
            "coding.net"
    ),
    TENCENT_SLS_STATIC_WEBSITE(
            "tencent-serverless-static-website",
            "depotName",
            "coding.net"
    );

    @Getter
    private final String projectName;
    @Getter
    private final String depotName;
    @Getter
    private final String codingDomain;

    TemplateEnums(String projectName, String depotName, String codingDomain) {
        this.projectName = projectName;
        this.depotName = depotName;
        this.codingDomain = codingDomain;
    }

    public static TemplateEnums string2enum(String s) {
        if (StringUtils.isEmpty(s)) {
            return null;
        }

        return Arrays.stream(TemplateEnums.values())
                .filter(t -> s.equalsIgnoreCase(t.name()))
                .findFirst()
                .orElse(null);

    }

    public boolean isTencentServerless() {
        return this == TENCENT_SLS_EXPRESS ||
                this == TENCENT_SLS_FLASK ||
                this == TENCENT_SLS_STATIC_WEBSITE;
    }

    public static List<String> getTencentServerless() {
        return Arrays.asList(TENCENT_SLS_EXPRESS.name(), TENCENT_SLS_FLASK.name(),
                TENCENT_SLS_STATIC_WEBSITE.name(), TENCENT_SLS_DB.name());
    }
}