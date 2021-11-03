package net.coding.lib.project.form.credential;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TencentServerlessCredentialForm extends BaseCredentialForm {
    private TencentServerlessCredentialRaw rawSlsCredential;
    private boolean fake;

    @Data
    public static class TencentServerlessCredentialRaw {
        private String secret_id;
        private String secret_key;
        private String token;
        private Long appid;
        private String signature;
        private Integer expired;
        private String uuid;
        private boolean success;
    }
}