package net.coding.lib.project;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.validation.Valid;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @ApiModelProperty("环境配置")
    @Valid
    @NotEmpty
    private String environment = "development";
    private String tokenUser = "project-token";
    private Credential credential = new Credential();
    private Domain domain = new Domain();
    private Icon icon = new Icon();

    public boolean isProd() {
        return environment.toLowerCase().startsWith("prod");
    }

    @Data
    @NoArgsConstructor
    public static class Credential {
        private String publicKey;
        private String privateKey;
        private boolean version2 = false;
    }

    @Data
    @NoArgsConstructor
    public static class Domain {
        private String home = "e.coding.net";
        private String main = "coding.net";
        private String schemaMain = "https://coding.net";
        private String schemaHome = "https://e.coding.net";
        private String pattern = "https://{0}.coding.net";
    }

    @Data
    @NoArgsConstructor
    public static class Icon {
        private String domain = "codehub.cn";
    }
}
