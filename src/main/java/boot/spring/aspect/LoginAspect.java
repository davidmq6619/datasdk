package boot.spring.aspect;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import boot.spring.pagemodel.AjaxResult;

/**
 * 登录校验的切面，若未登录则重定向到登录页面
 * @author shenzhanwang
 *
 */
@Component
@Aspect
public class LoginAspect {
	
	/*@Pointcut("execution(* boot.spring.controller.*.*(..)) && "
			+ "!execution(* boot.spring.controller.Login.*(..)) &&"
            + "!execution(* boot.spring.controller.JdbcTemplateController.*(..))")*/ // Login中写了登录方法
    public void login() {
    }
	
	
	/*@Around("login()")*/
    public Object auth(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取session中的用户信息
    	HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String username = (String) request.getSession().getAttribute("username");

        if (username == null) {
            //返回登录界面
            return "redirect:/login";
//        	return AjaxResult.error("请先登录");
        }
        return joinPoint.proceed();
    }
}
