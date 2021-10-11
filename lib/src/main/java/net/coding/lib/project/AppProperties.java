package net.coding.lib.project;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Data
@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @ApiModelProperty("环境配置")
    @Valid
    @NotEmpty
    String environment = "development";

    public boolean isProd() {
        return environment.toLowerCase().startsWith("prod");
    }

}
