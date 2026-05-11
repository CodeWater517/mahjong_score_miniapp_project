package com.example.mahjong;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.example.mahjong.module.**.mapper")
@SpringBootApplication
// 后端启动类：运行 main 方法后，Spring Boot 会扫描并启动整个服务。
public class MahjongApplication {

    public static void main(String[] args) {
        // 启动内置 Web 服务、加载配置、初始化数据库 Mapper、Controller、Service 等 Bean。
        SpringApplication.run(MahjongApplication.class, args);
    }
}
