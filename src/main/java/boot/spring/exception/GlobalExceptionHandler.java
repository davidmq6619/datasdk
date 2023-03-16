package boot.spring.exception;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import boot.spring.pagemodel.AjaxResult;


/**
 * 全局异常处理器
 * @author shenzhanwang
 *
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	
	@ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e) {
		LOG.error(e.getMessage(), e);
        return AjaxResult.error(e.getMessage());
    }
	
	@ExceptionHandler(RuntimeException.class)
    public AjaxResult notFount(RuntimeException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        String msg = String.format("访问的URL[%s]发生异常%s", requestURI, e.getMessage());
        LOG.error(msg, e);
        return AjaxResult.error(msg);
    }
}
