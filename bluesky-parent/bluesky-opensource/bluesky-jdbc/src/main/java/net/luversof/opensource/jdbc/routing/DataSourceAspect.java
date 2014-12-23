package net.luversof.opensource.jdbc.routing;

import java.lang.reflect.Method;

import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@Order(value=1)
public class DataSourceAspect {

	@Pointcut("@within(net.luversof.opensource.jdbc.routing.DataSource)")
	public void classPointcut() {
	}
	
	@Pointcut("@annotation(net.luversof.opensource.jdbc.routing.DataSource)")
	public void methodPointcut() {
	}
	
	@Before("classPointcut()")
	public void beforeClassPointcut(JoinPoint joinPoint) {
		log.debug("classPointcut activated");
		Class<?> targetClass = AopUtils.getTargetClass(joinPoint.getThis());
		DataSource dataSource = targetClass.getAnnotation(DataSource.class);
		if (dataSource == null) {
			return;
		}
		log.debug("beforeClassPointcut : {}", dataSource.value());
		DataSourceContextHolder.setDataSourceType(dataSource.value());
	}
	
	@Before("methodPointcut()")
	public void beforeMethodPointcut(JoinPoint joinPoint) {
		log.debug("methodPointcut activated");
		Signature signature = joinPoint.getSignature();
		if (!(signature instanceof MethodSignature)) {
			return;
		}
		
		Method method = ((MethodSignature) signature).getMethod();
		DataSource dataSource = (DataSource) method.getAnnotation(DataSource.class);
		
		if (dataSource == null) {
			return;
		}
		
		DataSourceContextHolder.setDataSourceType(dataSource.value());
	}
	
	@After("classPointcut()")
	public void afterClassPointcut(JoinPoint joinPoint) {
		log.debug("classPointcut clear");
		DataSourceContextHolder.clearDataSourceType();
	}
	
	@After("methodPointcut()")
	public void afterMethodPointcut(JoinPoint joinPoint) {
		log.debug("methodPointcut clear");
		DataSourceContextHolder.clearDataSourceType();
	}
}