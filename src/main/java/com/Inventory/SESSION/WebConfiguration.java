package com.Inventory.SESSION;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private final LoginSessionInterceptor loginSessionInterceptor;

    public WebConfiguration(LoginSessionInterceptor loginSessionInterceptor) {
        this.loginSessionInterceptor = loginSessionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginSessionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login", "/select-company", "/logout", "/error", "/favicon.ico",
                        "/css/**", "/js/**", "/images/**", "/webjars/**");
    }
}
