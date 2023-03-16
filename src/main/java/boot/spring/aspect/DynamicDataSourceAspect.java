package boot.spring.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import boot.spring.annotation.DataSource;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(0)
public class DynamicDataSourceAspect {

    private final Logger logger = LoggerFactory.getLogger(DynamicDataSourceAspect.class);

    @Pointcut("@annotation(boot.spring.annotation.DataSource)")
    public void annotationPointCut() { }

    @Before("annotationPointCut()")
    public void beforeSwitchDataSource(JoinPoint point) {

        Signature signature = point.getSignature();
        // 获得访问的方法名
        String methodName = signature.getName();
        // 得到方法的参数的类型
        Class[] argClass = ((MethodSignature) signature).getParameterTypes();
        String dataSource = null;
        try {
            // 获得当前访问的class
            Class<?> className = point.getTarget().getClass();

            // 得到访问的方法对象
            Method method = className.getMethod(methodName, argClass);

            // 判断是否存在@DataSource注解
            if (method.isAnnotationPresent(DataSource.class)) {
                DataSource annotation = method.getAnnotation(DataSource.class);
                // 取出注解中的数据源名
                dataSource = annotation.value();
            }
        } catch (Exception e) {
            logger.error("切换数据源:", e);
        }

        // 切换数据源
        DynamicDataSource.setDataSource(dataSource);

    }

    @After("annotationPointCut()")
    public void afterSwitchDataSource(JoinPoint point) {
        DynamicDataSource.clearDataSource();
    }
}
