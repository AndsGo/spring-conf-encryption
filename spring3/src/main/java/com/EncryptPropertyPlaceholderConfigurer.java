package com;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.util.Properties;


/**
 * 配置加密
 * @author songxulin
 * @date 2022/11/9
 */
public class EncryptPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {
    private String prefix = "HPO(";
    private String suffix = ")";
    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactory,
                                     Properties props) throws BeansException {
        try {
//            String encryption_key = System.getenv("XXX_KEY");
            String encryption_key = "12345678";
            for (Object key : props.keySet()) {
                if(isEncrypted(props.getProperty(key.toString()))){
                    props.setProperty(key.toString(), EncryptUtil.decrypt(unwrapEncryptedValue(props.getProperty(key.toString())),encryption_key));
                }
            }
            super.processProperties(beanFactory, props);
        } catch (Exception e) {
            throw new BeanInitializationException(e.getMessage());
        }
    }

    private boolean isEncrypted(String property) {
        if (property == null) {
            return false;
        }
        final String trimmedValue = property.trim();
        return (trimmedValue.startsWith(prefix) &&
                trimmedValue.endsWith(suffix));
    }


    private String unwrapEncryptedValue(String property) {
        return property.substring(
                prefix.length(),
                (property.length() - suffix.length()));
    }
}
