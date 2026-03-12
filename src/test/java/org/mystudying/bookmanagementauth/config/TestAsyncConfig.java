//package org.mystudying.bookmanagementauth.config;
//
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Primary;
//import org.springframework.scheduling.annotation.AsyncConfigurer;
//import org.springframework.scheduling.annotation.EnableAsync;
//
//import java.util.concurrent.Executor;
//
/// /@TestConfiguration
/// /public class TestAsyncConfig {
/// /
/// /    @Bean(name = "mailExecutor")
/// /    public Executor mailExecutor() {
/// /        return Runnable::run;
/// /    }
/// /}
//@TestConfiguration
//@EnableAsync
//public class TestAsyncConfig implements AsyncConfigurer {
//
//    @Override
//    public Executor getAsyncExecutor() {
//        return Runnable::run;
//    }
//}