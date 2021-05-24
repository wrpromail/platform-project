package net.coding.lib.project.enums;

import java.util.Arrays;

public enum CredentialTypeEnums {

    PASSWORD,
    USERNAME_PASSWORD,
    TOKEN,
    SECRET_KEY,
    APP_ID_SECRET_KEY,
    SSH,
    SSH_TOKEN,
    USERNAME_PASSWORD_TOKEN,
    CODING_PERSONAL_CREDENTIAL,
    OAUTH,
    KUBERNETES,
    ANDROID_CERTIFICATE,
    TENCENT_SERVERLESS,
    IOS_CERTIFICATE,
    TLS_CERTIFICATE;


    public enum CredentialByType {
        TGIT(new CredentialTypeEnums[]{USERNAME_PASSWORD, SSH_TOKEN, OAUTH}),
        GITHUB(new CredentialTypeEnums[]{USERNAME_PASSWORD, OAUTH}),
        GITLAB(new CredentialTypeEnums[]{USERNAME_PASSWORD, OAUTH}),
        GITEE(new CredentialTypeEnums[]{USERNAME_PASSWORD, OAUTH}),
        GITLAB_PRIVATE(new CredentialTypeEnums[]{USERNAME_PASSWORD, OAUTH}),
        ;

        private CredentialTypeEnums[] credentialType;

        CredentialByType(CredentialTypeEnums[] credentialType) {
            this.credentialType = credentialType;
        }

        public CredentialTypeEnums[] getCredentialType() {
            return credentialType;
        }
    }

    public static CredentialTypeEnums of(String name) {
        return Arrays.stream(CredentialTypeEnums.values())
                .filter(c -> c.name().equals(name))
                .findFirst()
                .orElse(PASSWORD);
    }
}
