package net.luversof.core.aspect;

import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;

/**
 * 이거 안되는거 이유가 궁금하네..
 * @author bluesky
 *
 */
@Slf4j
//@Aspect
//@Component
public class ExceptionAspect {
	
	@Pointcut("execution(* net.luversof..*.*Controller(..))")
	public void pointcut() {
	}

	@AfterThrowing(pointcut = "pointcut()", throwing = "error")
	public void afterThrowing(JoinPoint thisJoinPoint, Throwable error) throws Exception {
		Object args[] = thisJoinPoint.getArgs();
		
		log.warn("\n************ Exception Aspect Executed*****************");
		log.warn("\n* {}.{}()", thisJoinPoint.getTarget().getClass().getName(), thisJoinPoint.getSignature().getName());
		if (args.length > 0) {
			for (int i = 0 ; i < args.length ; i++) {
				log.warn("\n* {} arg's value : {}", (i+1), args[i].toString());
			}
		} else {
			log.warn("\n******************************************************");
		}
		log.debug(error.toString());
	}
	
	@Around("pointcut()")
	public void around(ProceedingJoinPoint pjp) {
		try {
			pjp.proceed();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
