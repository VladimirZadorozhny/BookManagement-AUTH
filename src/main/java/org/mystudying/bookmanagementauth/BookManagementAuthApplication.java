package org.mystudying.bookmanagementauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class
BookManagementAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookManagementAuthApplication.class, args);
    }

}


