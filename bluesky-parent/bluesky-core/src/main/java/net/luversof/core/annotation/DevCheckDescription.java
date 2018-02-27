package net.luversof.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DevCheckDescription {
	
	/**
	 * 설명 문구
	 * \/_check 또는 \/_check/util 개발 확인 페이지 호출 시 해당 설명 문구가 표시됨
	 * @return
	 */
	String value() default "";
	
	/**
	 * description 노출 여부
	 * @return
	 */
	boolean displayable() default true;
}
