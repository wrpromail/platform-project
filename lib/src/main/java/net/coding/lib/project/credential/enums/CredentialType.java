package net.coding.lib.project.credential.enums;

import java.util.Arrays;

public enum CredentialType {

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
        TGIT(new CredentialType[]{USERNAME_PASSWORD, SSH_TOKEN, OAUTH}),
        GITHUB(new CredentialType[]{USERNAME_PASSWORD, OAUTH}),
        GITLAB(new CredentialType[]{USERNAME_PASSWORD, OAUTH}),
        GITEE(new CredentialType[]{USERNAME_PASSWORD, OAUTH}),
        GITLAB_PRIVATE(new CredentialType[]{USERNAME_PASSWORD, OAUTH}),
        ;

        private final CredentialType[] credentialType;

        CredentialByType(CredentialType[] credentialType) {
            this.credentialType = credentialType;
        }

        public CredentialType[] getCredentialType() {
            return credentialType;
        }
    }

    public static CredentialType of(String name) {
        return Arrays.stream(CredentialType.values())
                .filter(c -> c.name().equals(name))
                .findFirst()
                .orElse(PASSWORD);
    }
}
