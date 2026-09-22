package com.booth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 本地文创市集摊位管理系统 - 启动类
 */
@SpringBootApplication
@MapperScan("com.booth.mapper")
public class BoothApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoothApplication.class, args);
        System.out.println("  >>> booth 后端启动成功: http://localhost:8080");
    }
}
