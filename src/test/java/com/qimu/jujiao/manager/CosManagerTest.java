package com.qimu.jujiao.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * Cos 操作测试
 */
@SpringBootTest
class CosManagerTest {

    @Resource
    private CosManager cosManager;

    @Test
    void putObject() {
        String fileName = "D:\\jujiao-backend\\src\\main\\resources\\课程表.png";
        cosManager.putObject("test.png", fileName);
    }
}