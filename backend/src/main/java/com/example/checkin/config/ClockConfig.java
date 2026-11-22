package com.example.checkin.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统一业务时钟。生产使用固定时区（Asia/Shanghai），测试可替换为 fixed/mutable Clock。
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${checkin.timezone:Asia/Shanghai}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}