package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(){

        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 允许携带的头信息
        corsConfiguration.addAllowedHeader("*");
        // 允许跨域的域名，可以写*,*代表所有的域名都可以访问，但不可携带cookie，所以不用
        corsConfiguration.addAllowedOrigin("http://manager.gmall.com");
        // 允许跨域的请求方法
        corsConfiguration.addAllowedMethod("*");
        // 是否允许携带cookie
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        //允许所有路径访问
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**" , corsConfiguration);
        return new CorsWebFilter(urlBasedCorsConfigurationSource);
    }
}
