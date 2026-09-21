package com.example.studyplan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.studyplan.mapper")
@SpringBootApplication
public class StudyPlanSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudyPlanSystemApplication.class, args);
    }
}
