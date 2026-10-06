package com.Inventory.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.Inventory.SESSION.SessionKeys;

/** Routes Spring JDBC calls to the company selected in the current HTTP session. */
public class CompanyRoutingDataSource extends AbstractDataSource {

    @Override
    public Connection getConnection() throws SQLException {
        return DBUtil1.getConnection(selectedCompany());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    private String selectedCompany() throws SQLException {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            throw new SQLException("Database access requires an active user session.");
        }
        Object company = attributes.getAttribute(SessionKeys.SELECTED_COMPANY, RequestAttributes.SCOPE_SESSION);
        if (!(company instanceof String)) {
            throw new SQLException("Select a company before using the application.");
        }
        try {
            return DBUtil1.canonicalCompany((String) company);
        } catch (IllegalArgumentException ex) {
            throw new SQLException("The selected company is invalid.", ex);
        }
    }
}
