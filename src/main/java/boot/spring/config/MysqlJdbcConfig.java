package boot.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.druid.pool.DruidDataSource;

@Configuration
@Component
public class MysqlJdbcConfig {
	
	@Bean(name="mysqldruid")
	public DruidDataSource druidDataSource() {
		DruidDataSource ds = new DruidDataSource();
		ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
		ds.setUrl("jdbc:mysql://localhost:3306/sakila?serverTimezone=Asia/Shanghai");
		ds.setUsername("root");
		ds.setPassword("root");
		return ds;
	}
	
	@Bean(name="mysqlTemplate")
	public JdbcTemplate getjdbcTemplate() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate();
		jdbcTemplate.setDataSource(druidDataSource());
		return jdbcTemplate;
	}
	
}
