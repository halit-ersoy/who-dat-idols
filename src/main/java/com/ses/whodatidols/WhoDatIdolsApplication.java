package com.ses.whodatidols;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class WhoDatIdolsApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhoDatIdolsApplication.class, args);
    }

}