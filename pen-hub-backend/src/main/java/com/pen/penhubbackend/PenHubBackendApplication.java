package com.pen.penhubbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true) // 开启AOP支持
public class PenHubBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(PenHubBackendApplication.class, args);
    }

}
