package net.coding.lib.project.form.credential;


import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TencentServerlessCredentialForm extends BaseCredentialForm {
    private TencentServerlessCredentialRaw rawSlsCredential;
    private boolean fake;

    @Data
    public class TencentServerlessCredentialRaw {
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