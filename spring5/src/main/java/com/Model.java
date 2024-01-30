package com;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author songxulin
 * @date 2024/1/27
 */
@Component
public class Model {
    @Value("${redis.host}")
    private String test;

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }
}
