package com.yxq.task.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES加密工具类
 * 用于数据库密码的加密存储
 */
public class AESUtil {
    // 加密算法
    private static final String KEY_ALGORITHM = "AES";
    // 默认加密算法
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
    // 加密密钥（实际项目中应放在配置文件中，并做好安全保护）
    private static final String SECRET_KEY = "FlinkCDC@Security";

    /**
     * 生成加密密钥
     *
     * @return 密钥
     */
    private static SecretKeySpec getSecretKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance(KEY_ALGORITHM);
            // 初始化密钥生成器
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(SECRET_KEY.getBytes());
            kg.init(128, random);
            SecretKey secretKey = kg.generateKey();
            return new SecretKeySpec(secretKey.getEncoded(), KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("生成密钥失败", e);
        }
    }

    /**
     * AES加密
     *
     * @param content 需要加密的内容
     * @return 加密后的Base64编码字符串
     */
    public static String encrypt(String content) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
            byte[] byteContent = content.getBytes(StandardCharsets.UTF_8);
            byte[] result = cipher.doFinal(byteContent);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * AES解密
     *
     * @param encryptedContent 加密的Base64编码字符串
     * @return 解密后的字符串
     */
    public static String decrypt(String encryptedContent) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey());
            byte[] result = cipher.doFinal(Base64.getDecoder().decode(encryptedContent));
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }
} 