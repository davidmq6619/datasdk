package boot.spring.aspect;

import java.time.LocalDateTime;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Component
@Aspect
public class LogAspect {
	
	private static final Logger LOG = LoggerFactory.getLogger(LogAspect.class);
	
	// 切面是所有接口
	@Pointcut("execution(* boot.spring.controller.*.*(..))")
    public void logPointcut() {
		
    }
	
    /**
     * 在执行方法前后调用Advice，这是最常用的方法，相当于@Before和@AfterReturning全部做的事儿
     * @param pjp
     * @return
     * @throws Throwable
     */
    @Around("logPointcut()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        // 接收到请求，记录请求内容
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        //打印请求的内容
        Long startTime = System.currentTimeMillis();
        LOG.info("请求Url : {}" , request.getRequestURL().toString());
        LOG.info("请求方式 : {}" , request.getMethod());
        LOG.info("请求ip : {}" , request.getRemoteAddr());
        LOG.info("请求参数 : {}" , Arrays.toString(pjp.getArgs()));
        Object ret = pjp.proceed();
        LOG.info("请求结束时间："+ LocalDateTime.now());
        LOG.info("请求方法 : " + pjp.getSignature().getDeclaringTypeName()+"." + pjp.getSignature().getName());
        LOG.info("请求耗时：{}" , (System.currentTimeMillis() - startTime));
        return ret;
    }
	
	
	/**
     * 异常通知：
     * 1. 在目标方法非正常结束，发生异常或者抛出异常时执行
     * 1. 在异常通知中设置异常信息，并将其保存
     * @param throwable
     */
    @AfterThrowing(value = "logPointcut()", throwing = "throwable")
    public void doAfterThrowing(Throwable throwable) {
        // 保存异常日志记录
    	LOG.error("发生异常时间：{}" + LocalDateTime.now());
    	LOG.error("抛出异常：{}" + throwable.getMessage());
    }
    
    //在事件通知类型中申明returning即可获取返回值
    @AfterReturning(value = "logPointcut()", returning="returnValue")
    public void logMethodCall(JoinPoint jp, Object returnValue) throws Throwable {
        LOG.info("方法返回值为：" + returnValue);
    }
}
