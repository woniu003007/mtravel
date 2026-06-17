package com.mtravel.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.mtravel.platform.**.mapper")
@SpringBootApplication
public class MtravelPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MtravelPlatformApplication.class, args);
    }
}
