package com;

import org.springframework.core.env.PropertySource;

/**
 * @author songxulin
 * @date 2024/1/27
 */
public interface EncryptablePropertySource<T> {

    PropertySource<T> getDelegate();

    Object getProperty(String name);
}
