package boot.spring.config;


import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.annotation.EnableTransactionManagement;



import boot.spring.aspect.DynamicDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/*@Configuration
@EnableTransactionManagement
@Order(1)
@ConfigurationProperties(prefix = "spring.datasource.hikari")
@MapperScan(value="boot.spring.mapper", sqlSessionFactoryRef = "sqlSessionFactory")*/
public class DataSourceConfiguration {

    /*private HikariDataSourceConfig sakila;

    private HikariDataSourceConfig rest;

    @Primary
    @Bean(name = "sakila")
    public DataSource sakila() {
        return sakila.getDataSource();
    }

    @Bean(name = "rest")
    public DataSource rest() {
        return rest.getDataSource();
    }

    @Bean(name = "dynamicDS")
    public DataSource dataSource() {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        // 默认数据源
        DataSource defaultTargetDataSource = sakila();
        dynamicDataSource.setDefaultTargetDataSource(defaultTargetDataSource);
        // 配置多数据源
        Map<Object, Object> dsMap = new HashMap<>(2);
        dsMap.put("sakila", defaultTargetDataSource);
        dsMap.put("rest", rest());

        dynamicDataSource.setTargetDataSources(dsMap);
        return dynamicDataSource;
    }

    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactoryBean(@Qualifier("dynamicDS") DataSource dynamicDS) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dynamicDS);
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        sqlSessionFactoryBean.setMapperLocations(resourceResolver.getResources("classpath:mapper/*.xml"));
        sqlSessionFactoryBean.getObject().getConfiguration().setMapUnderscoreToCamelCase(true);
        return sqlSessionFactoryBean.getObject();
    }

    public HikariDataSourceConfig getSakila() {
        return sakila;
    }

    public void setSakila(HikariDataSourceConfig sakila) {
        this.sakila = sakila;
    }

    public HikariDataSourceConfig getRest() {
        return rest;
    }

    public void setRest(HikariDataSourceConfig rest) {
        this.rest = rest;
    }
*/

}
