package net.luversof.blog.service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.luversof.blog.domain.mysql.TestAnnotation;

@Component
@Aspect
public class TestAop {
	
	@Autowired
	private Validator validator;
	
	//@Around("execution(* *(@net.luversof.blog.domain.mysql.TestAnnotation (*), ..)) && args(field, ..)")
	@Around(value = "@args(net.luversof.blog.domain.mysql.TestAnnotation)", argNames = "field")
	public void around(ProceedingJoinPoint joinPoint, Object field) throws Throwable {
		
		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        
        TestAnnotation targetAnnotation = null;
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for(Annotation[] annotations : parameterAnnotations){
        	for (Annotation annotation : annotations) {
        		if (annotation instanceof TestAnnotation) {
        			targetAnnotation = (TestAnnotation) annotation;
        		}
        	}
        }
        
//		Set<ConstraintViolation<Object>> result = validator.validate(field, targetAnnotation.value());
//		
//		if (!result.isEmpty()) {
//			System.out.println("-------------------------------------------AOP~!!! error");
//			throw new ConstraintViolationException(result);
//		}
//		joinPoint.proceed();
		System.out.println("-------------------------------------------AOP~!!!");
	}
}
