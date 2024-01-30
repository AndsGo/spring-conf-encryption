package com;

import org.springframework.core.env.PropertySource;

/**
 * @author songxulin
 * @date 2024/1/27
 */
public class EncryptablePropertySourceWrapper<T> extends PropertySource<T> implements EncryptablePropertySource<T> {
    private final PropertySource<T> encryptableDelegate;
    private final EncryptablePropertyResolver encryptablePropertyResolver;

    public EncryptablePropertySourceWrapper(PropertySource<T> delegate, EncryptablePropertyResolver resolver) {
        super(delegate.getName(), delegate.getSource());
        encryptableDelegate = delegate;
        encryptablePropertyResolver = resolver;
    }

    @Override
    public Object getProperty(String name) {
        Object value = encryptableDelegate.getProperty(name);
        if (value instanceof String) {
            String stringValue = String.valueOf(value);
            return encryptablePropertyResolver.resolvePropertyValue(stringValue);
        }
        return value;
    }

    @Override
    public PropertySource<T> getDelegate() {
        return encryptableDelegate;
    }
}
