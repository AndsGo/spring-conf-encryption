package com;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * @author songxulin
 * @date 2024/1/28
 */
public class DataSourceProperties extends Properties {
    private String location;
    /**
     * 构造方法
     *
     * @param location 需要解密的属性名称
     */
    public DataSourceProperties(String location) throws IOException {
        String[] split = location.split(",");
        for (String loc : split) {
            Properties properties = new Properties();
            properties.load(DataSourceProperties.class.getClassLoader()
                    .getResourceAsStream(loc));
            Enumeration<?> propertyNames = properties.propertyNames();
            while (propertyNames.hasMoreElements()) {
                String key = propertyNames.nextElement().toString();
                String value = properties.getProperty(key);
                this.setProperty(key, EncryptablePropertyResolver.resolvePropertyValue(value));
            }
        }
    }
}
