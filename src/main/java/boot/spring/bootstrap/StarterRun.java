package boot.spring.bootstrap;


import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


/**
 * 缓存预热，在spring boot启动时执行
 * 初始化单位树状结构到内存
 * @author shanzhanwang
 *
 */
@Order(value=1)
@Component
public class StarterRun implements ApplicationRunner {
	
	
	@Override
    public void run(ApplicationArguments args) throws Exception {
		System.out.println("服务启动成功");
    }

}
