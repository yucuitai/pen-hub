package com.pen.penhubbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true) // 开启AOP支持
@MapperScan("com.pen.penhubbackend.mapper")
public class PenHubBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(PenHubBackendApplication.class, args);
    }

}
