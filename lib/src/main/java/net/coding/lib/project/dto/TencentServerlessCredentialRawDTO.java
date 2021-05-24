package net.coding.lib.project.dto;

import com.google.gson.annotations.SerializedName;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class TencentServerlessCredentialRawDTO {
    @SerializedName(value = "SecretId")
    private String secretId;
    @SerializedName(value = "SecretKey")
    private String secretKey;
    private String token;
    @SerializedName(value = "AppId")
    private Long appId;
    private String signature;
    private Integer expired;
    private String uuid;
    private Integer timestamp;
}
