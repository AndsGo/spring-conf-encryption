package com;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author songxulin
 * @date 2024/1/27
 */
@Configuration
@PropertySource(value = {"classpath:constant.properties"})
public class JasyptSpringBootAutoConfiguration {
    @Bean
    public static EnableEncryptablePropertiesBeanFactoryPostProcessor enableEncryptablePropertySourcesPostProcessor(final StandardEnvironment environment) {
        return new EnableEncryptablePropertiesBeanFactoryPostProcessor(environment);
    }
}
