package net.coding.lib.project.utils;


import net.coding.lib.project.service.download.CodingSettings;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class XRsaUtil {
    public static final String CHARSET = "UTF-8";
    public static final String RSA_ALGORITHM = "RSA";
    public static final String RSA_OAEP_ALGORITHM = "RSA/ECB/OAEPWithSHA1AndMGF1Padding";
    public static final String RSA_ALGORITHM_SIGN = "SHA256WithRSA";
    private static RSAPublicKey publicKey;
    private static RSAPrivateKey privateKey;
    private final CodingSettings codingSettings;

    @PostConstruct
    public void init() {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            //通过X509编码的Key指令获得公钥对象
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(
                    Base64.decodeBase64(codingSettings.getApp().getCredential().getPublicKey())
            );
            publicKey = (RSAPublicKey) keyFactory.generatePublic(x509KeySpec);
            //通过PKCS#8编码的Key指令获得私钥对象
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(
                    Base64.decodeBase64(codingSettings.getApp().getCredential().getPrivateKey())
            );
            privateKey = (RSAPrivateKey) keyFactory.generatePrivate(pkcs8KeySpec);
        } catch (Exception e) {
            throw new RuntimeException("Unsupported key", e);
        }
    }

    public static Map<String, String> createKeys(int keySize) {
        //为RSA算法创建一个KeyPairGenerator对象
        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("No such algorithm-->[" + RSA_ALGORITHM + "]");
        }

        //初始化KeyPairGenerator对象,不要被initialize()源码表面上欺骗,其实这里声明的size是生效的
        kpg.initialize(keySize);
        //生成密匙对
        KeyPair keyPair = kpg.generateKeyPair();
        //得到公钥
        Key publicKey = keyPair.getPublic();
        String publicKeyStr = Base64.encodeBase64URLSafeString(publicKey.getEncoded());
        //得到私钥
        Key privateKey = keyPair.getPrivate();
        String privateKeyStr = Base64.encodeBase64URLSafeString(privateKey.getEncoded());
        Map<String, String> keyPairMap = new HashMap<>();
        keyPairMap.put("publicKey", publicKeyStr);
        keyPairMap.put("privateKey", privateKeyStr);

        return keyPairMap;
    }

    private static byte[] rsaSplitCodec(Cipher cipher, int opmode, byte[] datas, int keySize) {
        int maxBlock;
        if (opmode == Cipher.DECRYPT_MODE) {
            maxBlock = keySize / 8;
        } else {
            maxBlock = keySize / 8 - 11;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] buff;
        int i = 0;
        try {
            while (datas.length > offSet) {
                if (datas.length - offSet > maxBlock) {
                    buff = cipher.doFinal(datas, offSet, maxBlock);
                } else {
                    buff = cipher.doFinal(datas, offSet, datas.length - offSet);
                }
                out.write(buff, 0, buff.length);
                i++;
                offSet = i * maxBlock;
            }
        } catch (Exception e) {
            throw new RuntimeException("The encryption key data is abnormal", e);
        }
        byte[] resultDatas = out.toByteArray();
        IOUtils.closeQuietly(out);
        return resultDatas;
    }

    // 分块 with OAEP padding
    private static byte[] rsaSplitCodecOAEP(Cipher cipher, int opmode, byte[] datas, int keySize) {
        int maxBlock;
        if (opmode == Cipher.DECRYPT_MODE) {
            maxBlock = keySize / 8;
        } else {
            // A 1024-bit RSA key using OAEP padding can encrypt up to (1024/8) – 42 = 128 – 42 = 86 bytes.
            maxBlock = keySize / 8 - 42;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] buff;
        int i = 0;
        try {
            while (datas.length > offSet) {
                if (datas.length - offSet > maxBlock) {
                    buff = cipher.doFinal(datas, offSet, maxBlock);
                } else {
                    buff = cipher.doFinal(datas, offSet, datas.length - offSet);
                }
                out.write(buff, 0, buff.length);
                i++;
                offSet = i * maxBlock;
            }
        } catch (Exception e) {
            throw new RuntimeException("The encryption key data is abnormal", e);
        }
        byte[] resultDatas = out.toByteArray();
        IOUtils.closeQuietly(out);
        return resultDatas;
    }

    public String publicEncrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.encodeBase64URLSafeString(
                    rsaSplitCodec(
                            cipher,
                            Cipher.ENCRYPT_MODE,
                            data.getBytes(CHARSET),
                            publicKey.getModulus().bitLength()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Encrypted string data is abnormal", e);
        }
    }

    public static String publicEncrypt4Oaep(String data) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_OAEP_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, new OAEPParameterSpec(
                            "SHA-1",
                            "MGF1",
                            MGF1ParameterSpec.SHA1,
                            PSource.PSpecified.DEFAULT
                    )
            );
            byte[] b1 = cipher.doFinal(data.getBytes());
            return new String(Base64.encodeBase64(b1));
        } catch (Exception e) {
            throw new RuntimeException("Encrypted string data is abnormal", e);
        }
    }

    public static String publicEncrypt4OAEPv2(String data) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_OAEP_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, new OAEPParameterSpec(
                            "SHA-1",
                            "MGF1",
                            MGF1ParameterSpec.SHA1,
                            PSource.PSpecified.DEFAULT
                    )
            );
            return new String(
                    Base64.encodeBase64(rsaSplitCodecOAEP(
                            cipher,
                            Cipher.ENCRYPT_MODE,
                            data.getBytes(CHARSET),
                            publicKey.getModulus().bitLength()
                            )
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Encrypted string data is abnormal", e);
        }
    }

    public String privateDecrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return new String(
                    rsaSplitCodec(cipher,
                            Cipher.DECRYPT_MODE,
                            Base64.decodeBase64(data),
                            publicKey.getModulus().bitLength()
                    ),
                    CHARSET
            );
        } catch (Exception e) {
            throw new RuntimeException("Decrypted string data is abnormal", e);
        }
    }

    public static String privateDecrypt4Oaep(String data) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_OAEP_ALGORITHM);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    privateKey,
                    new OAEPParameterSpec(
                            "SHA-1",
                            "MGF1",
                            MGF1ParameterSpec.SHA1,
                            PSource.PSpecified.DEFAULT
                    )
            );
            return new String(cipher.doFinal(Base64.decodeBase64(data.getBytes())));
        } catch (Exception e) {
            throw new RuntimeException("Decrypted string data is abnormal", e);
        }
    }

    public static String privateDecrypt4OAEPv2(String data) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_OAEP_ALGORITHM);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    privateKey,
                    new OAEPParameterSpec(
                            "SHA-1",
                            "MGF1",
                            MGF1ParameterSpec.SHA1,
                            PSource.PSpecified.DEFAULT
                    )
            );
            return new String(
                    rsaSplitCodecOAEP(
                            cipher,
                            Cipher.DECRYPT_MODE,
                            Base64.decodeBase64(data.getBytes()),
                            publicKey.getModulus().bitLength()
                    ),
                    CHARSET
            );
        } catch (Exception e) {
            throw new RuntimeException("Decrypted string data is abnormal", e);
        }
    }

    public String privateEncrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return Base64.encodeBase64URLSafeString(
                    rsaSplitCodec(
                            cipher,
                            Cipher.ENCRYPT_MODE,
                            data.getBytes(CHARSET),
                            publicKey.getModulus().bitLength()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Encrypted string data is abnormal", e);
        }
    }

    public String publicDecrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            return new String(
                    rsaSplitCodec(cipher,
                            Cipher.DECRYPT_MODE,
                            Base64.decodeBase64(data),
                            publicKey.getModulus().bitLength()
                    ),
                    CHARSET
            );
        } catch (Exception e) {
            throw new RuntimeException("Decrypted string data is abnormal", e);
        }
    }

    public String sign(String data) {
        try {
            //sign
            Signature signature = Signature.getInstance(RSA_ALGORITHM_SIGN);
            signature.initSign(privateKey);
            signature.update(data.getBytes(CHARSET));
            return Base64.encodeBase64URLSafeString(signature.sign());
        } catch (Exception e) {
            throw new RuntimeException("Signature string data is abnormal", e);
        }
    }

    public boolean verify(String data, String sign) {
        try {
            Signature signature = Signature.getInstance(RSA_ALGORITHM_SIGN);
            signature.initVerify(publicKey);
            signature.update(data.getBytes(CHARSET));
            return signature.verify(Base64.decodeBase64(sign));
        } catch (Exception e) {
            throw new RuntimeException("Verification string data is abnormal", e);
        }
    }
}