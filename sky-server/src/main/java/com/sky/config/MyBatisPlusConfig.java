//package com.sky.config;
//
//import com.baomidou.mybatisplus.annotation.DbType;
//import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
//import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class MyBatisPlusConfig {
//
//    @Bean
//    public MybatisPlusInterceptor mybatisPlusInterceptor() {
//        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
//        // mysql分页插件，分页插件尽量放最后
//        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
//        paginationInnerInterceptor.setOverflow(true); // 页码超过总页数，返回最后一页
//        paginationInnerInterceptor.setMaxLimit(1000L); // 限制单页最大条数，防止查爆
//        interceptor.addInnerInterceptor(paginationInnerInterceptor);
//        return interceptor;
//    }
//}
