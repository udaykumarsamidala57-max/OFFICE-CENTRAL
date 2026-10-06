package com.Inventory.DBUtil;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.sql.DataSource;

@Configuration
public class AuthTransactionConfiguration {
    @Bean(name = "transactionManager")
    public PlatformTransactionManager companyTransactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "authTransactionManager")
    public PlatformTransactionManager authTransactionManager() {
        return new DataSourceTransactionManager(DBUtil1.getDataSource("SRS"));
    }
}
