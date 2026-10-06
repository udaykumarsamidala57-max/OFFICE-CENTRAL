package com.Inventory.DBUtil;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DatabaseConfiguration {

    @Bean
    @Primary
    public DataSource dataSource() {
        return new CompanyRoutingDataSource();
    }
}
