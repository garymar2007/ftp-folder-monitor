package com.gary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FtpFolderMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FtpFolderMonitorApplication.class);
    }
}