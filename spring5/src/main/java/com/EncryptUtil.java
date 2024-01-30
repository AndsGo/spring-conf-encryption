package com;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 *
 * @ClassName: EncryptUtil
 * @Description: (系统加密工具类)
 * @author wangyi
 */
public class EncryptUtil {

    /**
     *
     * decrypt (KEY解密方式)
     *
     * @param message
     * @return
     * @throws Exception
     *             设定文件
     */
    public static String decrypt(String message,String key) throws Exception {
        byte[] bytesrc = stringToBytes(message);
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        DESKeySpec desKeySpec = new DESKeySpec(key.getBytes("UTF-8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
        IvParameterSpec iv = new IvParameterSpec(key.getBytes("UTF-8"));

        cipher.init(2, secretKey, iv);

        byte[] retByte = cipher.doFinal(bytesrc);
        return new String(retByte, "UTF-8");
    }

    /**
     *
     * encrypt (KEY加密方式)
     *
     * @param message
     * @return
     * @throws Exception
     *             设定文件
     */
    public static String encrypt(String message,String key) throws Exception {
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");

        DESKeySpec desKeySpec = new DESKeySpec(key.getBytes("UTF-8"));

        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
        IvParameterSpec iv = new IvParameterSpec(key.getBytes("UTF-8"));
        cipher.init(1, secretKey, iv);

        String str = bytesToString(cipher.doFinal(message.getBytes("UTF-8")));
        return str;
    }

    private static byte[] stringToBytes(String temp) {
        byte[] digest = new byte[temp.length() / 2];
        for (int i = 0; i < digest.length; i++) {
            String byteString = temp.substring(2 * i, 2 * i + 2);
            int byteValue = Integer.parseInt(byteString, 16);
            digest[i] = ((byte) byteValue);
        }
        return digest;
    }

    private static String bytesToString(byte[] b) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            String plainText = Integer.toHexString(0xFF & b[i]);
            if (plainText.length() < 2) {
                plainText = "0" + plainText;
            }
            hexString.append(plainText);
        }
        return hexString.toString();
    }

}
