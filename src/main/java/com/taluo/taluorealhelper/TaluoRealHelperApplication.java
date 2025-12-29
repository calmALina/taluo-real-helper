package com.taluo.taluorealhelper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@MapperScan("com.taluo.taluorealhelper.mapper")
@SpringBootApplication
public class TaluoRealHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaluoRealHelperApplication.class, args);
    }

}
