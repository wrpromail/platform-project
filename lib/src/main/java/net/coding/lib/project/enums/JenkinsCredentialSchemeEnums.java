package net.coding.lib.project.enums;

import java.util.Arrays;

/**
 * jenkins credential type
 */
public enum JenkinsCredentialSchemeEnums {

    None(-1),
    UsernamePassword(0),
    DockerHostCertificateAuthentication(1),
    SSHUserNameWithPrivateKey(2),
    SecretFile(3),
    SecretText(4),
    Certificate(5),
    CloudApi(6);

    private int value;

    public int value() {
        return this.value;
    }

    JenkinsCredentialSchemeEnums(int value) {
        this.value = value;
    }

    public static JenkinsCredentialSchemeEnums nameOf(String name) {
        return Arrays.stream(JenkinsCredentialSchemeEnums.values())
                .filter(s -> s.name().equals(name))
                .findFirst()
                .orElse(None);
    }
}
