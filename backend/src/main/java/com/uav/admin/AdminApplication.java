package com.uav.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 无人机驾驶员管理后台 - 启动类
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication
@MapperScan("com.uav.admin.mapper")
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
        System.out.println("""
                =============================================
                  无人机驾驶员管理后台 启动成功
                  API 文档: http://localhost:8080/swagger-ui.html
                =============================================
                """);
    }
}
