package boot.spring.config;

import boot.spring.constant.SdkConstant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.druid.pool.DruidDataSource;

@Configuration
@Component
public class PgJdbcConfig {
	
	@Bean(name="pgdruid")
	public DruidDataSource druidDataSource() {
		DruidDataSource ds = new DruidDataSource();
		ds.setDriverClassName(SdkConstant.JDBC_DRIVER);
		/*ds.setUrl("jdbc:postgresql://192.168.16.127:5432/aimdthistory");
		ds.setUsername("aimdt");
		ds.setPassword("zk@yR6l1y99sx@Group");*/
		ds.setUrl(SdkConstant.JDBC_URL);
		ds.setUsername(SdkConstant.JDBC_NAME);
		ds.setPassword(SdkConstant.JDBC_PWD);
		return ds;
	}
	
	@Bean(name="pgTemplate")
	public JdbcTemplate getjdbcTemplate() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate();
		jdbcTemplate.setDataSource(druidDataSource());
		return jdbcTemplate;
	}
}
