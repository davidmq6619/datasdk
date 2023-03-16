package boot.spring.config;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadPool {
	@Value("${threadpool.coresize}")
	private Integer corePoolSize;
	
	@Value("${threadpool.maxsize}")
	private Integer maxPoolSize;
	
	@Value("${threadpool.alivetime}")
	private Long keepAlivedTime;
	
	@Value("${threadpool.queuesize}")
	private Integer queuesize;
	
	@Bean
	public ThreadPoolExecutor threadPoolExecutor() {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAlivedTime, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(queuesize));
		return executor;
	}
	
}