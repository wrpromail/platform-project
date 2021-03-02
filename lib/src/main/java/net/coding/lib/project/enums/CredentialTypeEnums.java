package net.coding.lib.project.enums;

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

        public String[] getByNames() {
            CredentialByType credentialByType = CredentialByType.valueOf(this.name());
            CredentialTypeEnums[] credentialTypes = credentialByType.getCredentialType();
            String[] names = new String[credentialTypes.length];
            for (int i = 0; i < credentialTypes.length; i++) {
                names[i] = credentialTypes[i].name();
            }
            return names;
        }

        public boolean isSupportAuthType(String credentialType) {
            CredentialByType credentialByType = CredentialByType.valueOf(this.name());
            CredentialTypeEnums[] credentialTypes = credentialByType.getCredentialType();
            for (int i = 0; i < credentialTypes.length; i++) {
                if (credentialTypes[i].name().equalsIgnoreCase(credentialType)) {
                    return true;
                }
            }
            return false;
        }

    }

    public static CredentialTypeEnums of(String name) {
        for (CredentialTypeEnums type : CredentialTypeEnums.values()) {
            if (type.name() == name) {
                return type;
            }
        }
        return PASSWORD;
    }
}
