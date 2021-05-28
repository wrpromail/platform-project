package net.coding.lib.project.service.credential;


import com.google.gson.Gson;

import net.coding.common.base.gson.JSON;
import net.coding.common.util.BeanUtils;
import net.coding.common.util.StringUtils;
import net.coding.lib.project.dao.credentail.ProjectCredentialDao;
import net.coding.lib.project.dao.credentail.TencentServerlessCredentialsDao;
import net.coding.lib.project.dto.TencentServerlessCredentialRawDTO;
import net.coding.lib.project.entity.Credential;
import net.coding.lib.project.entity.TencentServerlessCredential;
import net.coding.lib.project.form.credential.TencentServerlessCredentialForm;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class TencentServerlessCredentialService {
    public static final String API_BASE_URL = "scfdev.tencentserverless.com";
    public static final String REFRESH_TOKEN_PATH = "/login/info";
    private final TencentServerlessCredentialsDao tencentServerlessCredentialsDao;
    private final ProjectCredentialDao projectCredentialDao;
    private final RestTemplate restTemplate;
    private  final  Gson gson;


    public Credential flushIfNeed(Credential credential) {
        TencentServerlessCredential serverlessCredential = tencentServerlessCredentialsDao.getByConnId(
                credential.getId(),
                BeanUtils.getDefaultDeletedAt()
        );
        int considerExpiredUnixTime = (int) LocalDateTime.now().minusMinutes(5).toEpochSecond(ZoneOffset.of("+8"));
        if (serverlessCredential.getWasted()) {
            return credential;
        }
        if (serverlessCredential.getExpired() > considerExpiredUnixTime) {
            return credential;
        }
        TencentServerlessCredentialForm.TencentServerlessCredentialRaw raw = requestFlush(serverlessCredential);
        if (raw.isSuccess()) {
            serverlessCredential.setSignature(raw.getSignature());
            serverlessCredential.setToken(raw.getToken());
            serverlessCredential.setSecretId(raw.getSecret_id());
            serverlessCredential.setSecretKey(raw.getSecret_key());
            serverlessCredential.setExpired(raw.getExpired());
            tencentServerlessCredentialsDao.updateCredential(serverlessCredential);
            raw.setUuid(serverlessCredential.getUuid());
            String secretKey = toJSON(raw);
            credential.setSecretKey(secretKey);
            projectCredentialDao.updateSecretKey(
                    credential.getId(),
                    secretKey,
                    BeanUtils.getDefaultDeletedAt()
            );
            return credential;
        }
        tencentServerlessCredentialsDao.updateWasted(serverlessCredential.getId(), true);
        String secretKey = "{}";
        credential.setSecretKey(secretKey);
        projectCredentialDao.updateSecretKey(
                credential.getId(),
                secretKey,
                BeanUtils.getDefaultDeletedAt());
        return credential;
    }

    public String toJSON(TencentServerlessCredentialForm.TencentServerlessCredentialRaw raw) {
        if (StringUtils.isBlank(raw.getSecret_key())) {
            return "{}";
        }
        return gson.toJson(
                TencentServerlessCredentialRawDTO.builder()
                        .secretId(raw.getSecret_id())
                        .secretKey(raw.getSecret_key())
                        .token(raw.getToken())
                        .appId(raw.getAppid())
                        .expired(raw.getExpired())
                        .signature(raw.getSignature())
                        .uuid(raw.getUuid())
                        .timestamp(LocalDateTime.now().getSecond())
                        .build()
        );
    }

    private TencentServerlessCredentialForm.TencentServerlessCredentialRaw requestFlush(TencentServerlessCredential serverlessCredential) {
        String responseText="{}";
        String url = String.format(
                "http://%s%s?uuid=%s&expired=%d&signature=%s&appid=%s&os=Linux",
                API_BASE_URL, REFRESH_TOKEN_PATH, serverlessCredential.getUuid(),
                serverlessCredential.getExpired(), serverlessCredential.getSignature(),
                serverlessCredential.getAppId()
        );
        try {
            responseText = restTemplate.getForObject(url, String.class);
        }catch (Exception e){
            log.error("Http get request fail {} ", e.getMessage());
        }
        return JSON.fromJson(responseText, TencentServerlessCredentialForm.TencentServerlessCredentialRaw.class);
    }
}
