package com;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;


/**
 * @author songxulin
 * @date 2024/1/27
 */
public class EnableEncryptablePropertiesBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {

    private final ConfigurableEnvironment environment;

    public EnableEncryptablePropertiesBeanFactoryPostProcessor(StandardEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        MutablePropertySources propSources = this.environment.getPropertySources();
        StreamSupport.stream(propSources.spliterator(), false)
                .filter(ps -> !(ps instanceof EncryptablePropertySource))
                .map(this::makeEncryptable)
                .collect(toList())
                .forEach(ps -> propSources.replace(ps.getName(), ps));
    }

    @Override
    public int getOrder() {
        return 2147483547;
    }

    /**
     * 包装自定义的PropertySource指定解析器
     * @param propertySource
     * @param <T>
     * @return
     */
    public <T> PropertySource<T> makeEncryptable(PropertySource<T> propertySource) {
        if (propertySource instanceof EncryptablePropertySource ) {
            return propertySource;
        }
        PropertySource<T> encryptablePropertySource = new EncryptablePropertySourceWrapper(propertySource,new EncryptablePropertyResolver());
        return encryptablePropertySource;
    }
}
