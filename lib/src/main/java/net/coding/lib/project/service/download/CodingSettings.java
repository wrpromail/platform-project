package net.coding.lib.project.service.download;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "coding")
public class CodingSettings {
    private App app = new App();
    private Enterprise enterprise = new Enterprise();
    private String ipWhiteNamePreset;
    private ApiDoc apiDoc = new ApiDoc();
    /**
     * 测试管理rest服务配置信息
     */
    private Testing testing = new Testing();

    @Getter
    @Setter
    public static class App {
        private boolean ssoEnabled = false;
        private String ssoType = "";
        private String protocol = "https";
        private String host = "e.coding.net";
        private String gitHost = "e.coding.net";
        private String gitHostTencent = "git.dev.tencent.com";
        private String teamHost = "%s.coding.net";
        private String svnHost = "svn.coding.net";
        private String svnHttpHost = "svn.coding.net";
        private String subversionHost = "subversion.coding.net";
        private String owner = "coding";
        private String title = "CODING";
        private String logo = "https://dn-coding-net-production-pp.codehub.cn/032ccb31-a783-41ba-bce9-611930a08ade.png";
        private String logoAlt = "CODING";
        private Email email = new Email();
        private Ide ide = new Ide();
        private Image image = new Image();
        private Credential credential = new Credential();

        public String hostWithProtocol() {
            return protocol + "://" + host;
        }

        public String gitHostWithProtocol() {
            return protocol + "://" + gitHost;
        }

        public String teamHostWithProtocol() {
            return protocol + "://" + teamHost;
        }

        public boolean isCoding() {
            return owner.equals("coding");
        }

        public String svnHostWithProtocol() {
            return protocol + "://" + svnHttpHost;
        }

        @Getter
        @Setter
        public static class Email {
            private String logo = "https://dn-coding-net-production-pp.codehub.cn/ad06ae29-41c5-4df6-ae25-850caf7c3ba6.png";
            private String supportLink = "support@coding.net";
            private String enterpriseLink = "p_phangchen@tencent.com";
        }

        @Getter
        @Setter
        public static class Ide {
            private boolean showLink = false;
            private String link = "/cs/dashboard";
        }

        @Getter
        @Setter
        public static class Image {
            private String notFoundImage = "https://coding.net/static/no-pic.png";
            private String domainPrefix1 = "https://dn-coding-";
            private String domainPrefix2 = "https://coding-";
            private String subDomain = "coding";
        }

        @Getter
        @Setter
        public static class Credential {
            private String publicKey;
            private String privateKey;
            private boolean version2 = false;
        }
    }

    @Getter
    @Setter
    public static class Enterprise {
        private Double oldUnitPrice = 1.00; // price per day
        private Double yearUnitStandardPrice = 399.00;// price per year
        private Double yearUnitAdvancedPrice = 599.00;// price per year
        private Double daysOfYear = 365.0;
        private Double unitPrice = yearUnitStandardPrice / daysOfYear; // price per day for standard
        private Double unitAdvancedPrice = yearUnitAdvancedPrice / daysOfYear; // price per day for advanced
        private Integer bonusPercent = 0; // eg: 20, means: 20%
        private Integer trialDays = 14; // 试用期为 15 天，到期日 - 订购日 = 14 天
        private Integer extraTrialDays = 40; // 特殊时期的试用期
        private String extraTrialStartDatePoint = "2020-07-31"; //  特殊时期开始点
        private String extraTrialEndDatePoint = "2020-09-11"; //  特殊时期结束点
        private String chargeStartDatePoint = "2020-09-11"; //  新计费启用时间，对于资源配额部分会通过此时间比较是否使用老版资源配额
        private Boolean enable = false;
        private Integer maxInviteCountPerDay = 50;
        private Integer freeMemberCount = 5;
        private Integer totalInviteCountPerDay = 500;
        private String emailTitle = "[通知] CODING 提醒邮件";
        private String feieUrlPrefix = "https://cps-admin.feie.work/callback/coding/admin/auth?";
        private String feieSignCode = "coding";
        private String tokenUser = "project-token";
        private String tokenUserAvatar = "https://coding-net-production-pp-ci.codehub.cn/f4712b37-f520-4550-9d06-414e6c78c261.png";
        private String tokenUsername = "项目助手";
        private Integer maxGrade = 3000; // 订单档位上限
    }

    @Getter
    @Setter
    public static class ApiDoc {
        private String serviceName = "e-api-docs-backend-svc";
        private String servicePort = "8394";
        private String serviceToken = "p9KCaV6JSq8mYwB";
    }

    @Getter
    @Setter
    public static class Testing {
        private String serviceName = "coding-testing";
        private String servicePort = "8080";
    }
}
