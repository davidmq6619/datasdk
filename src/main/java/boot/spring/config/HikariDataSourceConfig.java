package boot.spring.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import org.springframework.context.annotation.Configuration;
public class HikariDataSourceConfig {

    private String driverClassName;
    private String jdbcUrl;
    private volatile String username;
    private volatile String password;
    private volatile String poolName;
    private volatile int minimumIdle = 5;
    private volatile int maximumPoolSize = 15;
    private volatile boolean autoCommit = true;
    private volatile int idleTimeout = 30000;
    private volatile int maxLifetime = 1800000;
    private volatile int connectionTimeout = 30000;
    private volatile String connectionTestQuery = "SELECT 1";


    private  HikariDataSource dataSourceBean;

    public DataSource getDataSource(){
        if(dataSourceBean == null) {
            synchronized (HikariDataSourceConfig.class) {
                HikariConfig hikariConfig = new HikariConfig();
                hikariConfig.setDriverClassName(driverClassName);
                hikariConfig.setJdbcUrl(jdbcUrl);
                hikariConfig.setUsername(username);
                hikariConfig.setPassword(password);
                hikariConfig.setPoolName(poolName);
                hikariConfig.setMinimumIdle(minimumIdle);
                hikariConfig.setMaximumPoolSize(maximumPoolSize);
                hikariConfig.setAutoCommit(autoCommit);
                hikariConfig.setIdleTimeout(idleTimeout);
                hikariConfig.setMaxLifetime(maxLifetime);
                hikariConfig.setConnectionTimeout(connectionTimeout);
                hikariConfig.setConnectionTestQuery(connectionTestQuery);

                dataSourceBean = new HikariDataSource(hikariConfig);

            }
        }
        return dataSourceBean;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public int getMinimumIdle() {
        return minimumIdle;
    }

    public void setMinimumIdle(int minimumIdle) {
        this.minimumIdle = minimumIdle;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public int getMaxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(int maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getConnectionTestQuery() {
        return connectionTestQuery;
    }

    public void setConnectionTestQuery(String connectionTestQuery) {
        this.connectionTestQuery = connectionTestQuery;
    }



}