package net.coding.lib.project.credential.enums;

import java.util.Arrays;

/**
 * jenkins credential type
 */
public enum CredentialJenkinsScheme {

    None(-1),
    UsernamePassword(0),
    DockerHostCertificateAuthentication(1),
    SSHUserNameWithPrivateKey(2),
    SecretFile(3),
    SecretText(4),
    Certificate(5),
    CloudApi(6);

    private final int value;

    public int value() {
        return this.value;
    }

    CredentialJenkinsScheme(int value) {
        this.value = value;
    }

    public static CredentialJenkinsScheme nameOf(String name) {
        return Arrays.stream(CredentialJenkinsScheme.values())
                .filter(s -> s.name().equals(name))
                .findFirst()
                .orElse(None);
    }
}
