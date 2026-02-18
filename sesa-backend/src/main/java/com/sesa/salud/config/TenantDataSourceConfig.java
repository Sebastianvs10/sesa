/**
 * Autor: Ing. J Sebastian Vargas S
 */

package com.sesa.salud.config;

import com.sesa.salud.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Envuelve el DataSource para que cada conexión use el esquema del tenant actual.
 */
@Slf4j
@Configuration
public class TenantDataSourceConfig {

    @Bean
    public BeanPostProcessor dataSourceWrapper() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DataSource && !(bean instanceof TenantAwareDataSource)) {
                    return new TenantAwareDataSource((DataSource) bean);
                }
                return bean;
            }
        };
    }

    private static class TenantAwareDataSource implements DataSource {

        private final DataSource delegate;

        TenantAwareDataSource(DataSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection conn = delegate.getConnection();
            String schema = TenantContextHolder.getTenantSchema();
            try {
                conn.setSchema(schema);
            } catch (SQLException e) {
                log.warn("No se pudo establecer schema {}: {}", schema, e.getMessage());
            }
            return conn;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Connection conn = delegate.getConnection(username, password);
            String schema = TenantContextHolder.getTenantSchema();
            try {
                conn.setSchema(schema);
            } catch (SQLException e) {
                log.warn("No se pudo establecer schema {}: {}", schema, e.getMessage());
            }
            return conn;
        }

        @Override
        public java.io.PrintWriter getLogWriter() throws SQLException {
            return delegate.getLogWriter();
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) throws SQLException {
            delegate.setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            delegate.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return delegate.getLoginTimeout();
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getLogger(DataSource.class.getName());
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }
            return delegate.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return iface.isInstance(this) || delegate.isWrapperFor(iface);
        }
    }
}
