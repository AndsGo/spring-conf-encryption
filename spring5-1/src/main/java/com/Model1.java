package com;

import org.springframework.stereotype.Component;

/**
 * @author songxulin
 * @date 2024/1/27
 */
@Component
public class Model1 {
    private String test;
    private String test2;

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public String getTest2() {
        return test2;
    }

    public void setTest2(String test2) {
        this.test2 = test2;
    }
}
