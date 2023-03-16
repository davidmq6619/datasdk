package boot.spring.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Configuration
@Component
public class MeinianJdbcConfig {
	
	@Bean(name="meinianDruid")
	public DruidDataSource druidDataSource() {
		DruidDataSource ds = new DruidDataSource();
		ds.setDriverClassName("org.postgresql.Driver");
		ds.setUrl("jdbc:postgresql://192.168.16.57:54321/whe_user");
		ds.setUsername("whe");
		ds.setPassword("whe");
		return ds;
	}
	
	@Bean(name="meinianTemplate")
	public JdbcTemplate getjdbcTemplate() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate();
		jdbcTemplate.setDataSource(druidDataSource());
		return jdbcTemplate;
	}
}
