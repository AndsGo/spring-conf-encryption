package com;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;

import java.util.Optional;

/**
 * 配置加密
 *
 * @author songxulin
 * @date 2022/11/9
 */
public class EncryptablePropertyResolver {
    private static String prefix = "HPO(";
    private static String suffix = ")";

    public static String resolvePropertyValue(String value) throws BeansException {
        return Optional.ofNullable(value)
                .filter(EncryptablePropertyResolver::isEncrypted)        // 如果经过属性探测器确认的，才继续
                .map(resolvedValue -> {
                    try {
        //            String encryption_key = System.getenv("XXX_KEY");
                        String encryption_key = "12345678";
                        return EncryptUtil.decrypt(EncryptablePropertyResolver.unwrapEncryptedValue(value), encryption_key);
                    } catch (Exception e) {
                        throw new BeanInitializationException(e.getMessage());
                    }
                })
                .orElse(value);
    }

    private static boolean isEncrypted(String property) {
        if (property == null) {
            return false;
        }
        final String trimmedValue = property.trim();
        return (trimmedValue.startsWith(prefix) &&
                trimmedValue.endsWith(suffix));
    }


    private static String unwrapEncryptedValue(String property) {
        return property.substring(
                prefix.length(),
                (property.length() - suffix.length()));
    }
}
