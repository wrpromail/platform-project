package net.coding.lib.project.service.credential;

import net.coding.lib.project.AppProperties;
import net.coding.lib.project.entity.AndroidCredential;
import net.coding.lib.project.entity.Credential;
import net.coding.lib.project.enums.CredentialTypeEnums;
import net.coding.lib.project.utils.XRsaUtil;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class ProjectCredentialRsaService {
    private final AppProperties appProperties;

    public void encrypt(Credential credential) {
        CredentialTypeEnums credentialType = CredentialTypeEnums.valueOf(credential.getType());
        if (credentialType == CredentialTypeEnums.PASSWORD ||
                credentialType == CredentialTypeEnums.USERNAME_PASSWORD ||
                credentialType == CredentialTypeEnums.USERNAME_PASSWORD_TOKEN) {
            String password = credential.getPassword();
            if (StringUtils.isBlank(password)) {
                return;
            }
            credential.setPassword(encrypt(password.trim()));
        }
    }

    public String encrypt(String text) {
        if (text == null) {
            return StringUtils.EMPTY;
        }
        if (appProperties.getCredential().isVersion2()) {
            return XRsaUtil.publicEncrypt4OAEPv2(text);
        }
        return XRsaUtil.publicEncrypt4Oaep(text);
    }

    /**
     * AndroidCredential 的
     */
    public void encrypt(AndroidCredential androidCert) {
        String password = androidCert.getFilePassword();
        String aliasPassword = androidCert.getAliasPassword();
        if (StringUtils.isNotBlank(password)) {
            androidCert.setFilePassword(encrypt(password.trim()));
        }
        if (StringUtils.isNotBlank(aliasPassword)) {
            androidCert.setAliasPassword(encrypt(aliasPassword.trim()));
        }
    }

    public void decrypt(Credential credential) {
        decrypt(credential, null);
    }

    public void decrypt(AndroidCredential androidCert) {
        String password = androidCert.getFilePassword();
        String aliasPassword = androidCert.getAliasPassword();
        if (StringUtils.isNotBlank(password)) {
            androidCert.setFilePassword(decrypt(password.trim()));
        }
        if (StringUtils.isNotBlank(aliasPassword)) {
            androidCert.setAliasPassword(decrypt(aliasPassword.trim()));
        }
    }

    private void decrypt(Credential credential, Consumer<Credential> failHandler) {
        CredentialTypeEnums credentialType = CredentialTypeEnums.valueOf(credential.getType());
        if (credentialType == CredentialTypeEnums.PASSWORD ||
                credentialType == CredentialTypeEnums.USERNAME_PASSWORD ||
                credentialType == CredentialTypeEnums.USERNAME_PASSWORD_TOKEN) {
            String originPwd = credential.getPassword();
            if (StringUtils.isBlank(originPwd)) {
                return;
            }
            String password = originPwd.trim();
            try {
                credential.setPassword(decrypt(password));
            } catch (Exception e) {
                log.warn("Failed to decrypt credential {} password, may be origin text", credential.getId());
                if (failHandler != null) {
                    failHandler.accept(credential);
                }
            }
        }
    }

    public String decrypt(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        if (appProperties.getCredential().isVersion2()) {
            return XRsaUtil.privateDecrypt4OAEPv2(text.trim());
        }
        return XRsaUtil.privateDecrypt4Oaep(text.trim());
    }

}
